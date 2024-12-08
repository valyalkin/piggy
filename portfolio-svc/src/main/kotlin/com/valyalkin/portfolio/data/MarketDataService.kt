package com.valyalkin.portfolio.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.valyalkin.portfolio.configuration.BusinessException
import com.valyalkin.portfolio.configuration.SystemException
import com.valyalkin.portfolio.data.eod.EndOfDayPriceDataEntity
import com.valyalkin.portfolio.data.eod.EndOfDayPriceDataRepository
import com.valyalkin.portfolio.data.tickers.TickersEntity
import com.valyalkin.portfolio.data.tickers.TickersRepository
import com.valyalkin.portfolio.stocks.transactions.Country
import com.valyalkin.portfolio.stocks.transactions.Currency
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.io.IOException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Service
class MarketDataService(
    @Value("\${data.marketstack.url}") private val marketStackUrl: String,
    @Value("\${data.marketstack.apikey}") private val marketStackApiKey: String,
    private val endOfDayPriceDataRepository: EndOfDayPriceDataRepository,
    private val tickersRepository: TickersRepository,
) {
    private val client =
        RestClient
            .builder()
            .build()

    private val countryToCurrencyMap =
        mapOf(
            Country.US to Currency.USD,
            Country.SG to Currency.SGD,
        )

    private fun fetchEndOfDayData(
        dateFrom: LocalDate,
        offset: Int,
        symbols: String,
    ): MarketStackEndOfDayPrices {
        val url =
            UriComponentsBuilder
                .fromUriString("$marketStackUrl/v1/eod")
                .queryParam("access_key", marketStackApiKey)
                .queryParam("symbols", symbols)
                .queryParam("date_from", dateFrom.toString())
                .queryParam("limit", 100)
                .queryParam("offset", offset)
                .toUriString()

        return client
            .get()
            .uri(url)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, response ->
                throw SystemException(
                    "Marketstack api call failed with error ${response.statusCode.value()}," +
                        " Details: ${response.body.readAllBytes().toString(Charset.defaultCharset())}",
                )
            }.body(MarketStackEndOfDayPrices::class.java)
            ?: throw SystemException("Not able to fetch the data from marketstack")
    }

    private fun fetchTickerData(ticker: String): MarketStackTicker {
        val url =
            UriComponentsBuilder
                .fromUriString("$marketStackUrl/v1/tickers/$ticker")
                .queryParam("access_key", marketStackApiKey)
                .toUriString()

        return client
            .get()
            .uri(url)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError) { _, response ->
                logger.error(
                    "Marketstack api call failed with error ${response.statusCode.value()}," +
                        " Details: ${response.body.readAllBytes().toString(Charset.defaultCharset())}",
                )
                throw BusinessException(
                    "Ticker $ticker is not found or not supported",
                )
            }.onStatus(HttpStatusCode::is5xxServerError) { _, response ->
                throw SystemException(
                    "Marketstack api call failed with error ${response.statusCode.value()}," +
                        " Details: ${response.body.readAllBytes().toString(Charset.defaultCharset())}",
                )
            }.body(MarketStackTicker::class.java)
            ?: throw SystemException("Not able to fetch the data")
    }

    fun getTickerData(ticker: String): TickerData {
        // Retrieve ticker information if already stored
        val tickerEntity = tickersRepository.findBySymbol(symbol = ticker)

        tickerEntity?.let {
            return TickerData(
                ticker = ticker,
                currency = it.currency,
            )
        }

        val marketStackTicker = fetchTickerData(ticker)

        val countryEnum =
            try {
                Country.valueOf(marketStackTicker.stockExchange.countryCode.uppercase())
            } catch (e: IllegalArgumentException) {
                throw BusinessException("Country ${marketStackTicker.stockExchange.countryCode} currently is not supported")
            }
        val currency =
            countryToCurrencyMap[countryEnum]
                ?: throw BusinessException("Country ${marketStackTicker.stockExchange.country} is currently not supported")

        tickersRepository.save(
            TickersEntity(
                symbol = ticker,
                currency = currency,
                name = marketStackTicker.name,
                hasEodPrice = marketStackTicker.hasEod,
                exchange = marketStackTicker.stockExchange.name,
                acronym = marketStackTicker.stockExchange.acronym,
                mic = marketStackTicker.stockExchange.mic,
                country = marketStackTicker.stockExchange.country,
                countryCode = countryEnum,
            ),
        )

        return TickerData(
            ticker = ticker,
            currency = currency,
        )
    }

    fun validateCurrency(
        ticker: String,
        currency: Currency,
    ) {
        val tickerEntity = tickersRepository.findBySymbol(symbol = ticker)
        tickerEntity?.let {
            if (currency != tickerEntity.currency) {
                throw BusinessException("Incorrect currency for ticker $ticker, correct currency is ${tickerEntity.currency}")
            }
        } ?: throw BusinessException("Ticker $ticker is not found")
    }

    fun getLatestEodPrice(ticker: String): Pair<BigDecimal, LocalDate> {
        val entity = endOfDayPriceDataRepository.findLatestPriceForTicker(ticker)

        entity?.let {
            return Pair(entity.price, entity.date)
        } ?: throw SystemException("Cannot find latest price for ticker $ticker")
    }

    fun processEndOfDayData() {
        val list = mutableListOf<MarketStackEndOfDayData>()

        val latestDate = endOfDayPriceDataRepository.findLatestPriceDateForTicker("AAPL")
        val symbols = "AAPL,NET"

        val dateFrom =
            if (latestDate == null) {
                LocalDate.of(2019, 1, 1)
            } else {
                latestDate.plusDays(1)
            }

        val result =
            fetchEndOfDayData(
                dateFrom = dateFrom,
                offset = 0,
                symbols = symbols,
            )

        list.addAll(result.data)
        val pagination = result.pagination

        val numCalls = ceil(pagination.total.toDouble() / pagination.limit).toInt()

        if (numCalls != 0) {
            for (i in 1..numCalls) {
                val offset = i * pagination.limit
                val eod = fetchEndOfDayData(dateFrom = dateFrom, offset = offset, symbols = symbols)
                list.addAll(
                    eod.data,
                )
            }
        }

        logger.info(list.toString())
        logger.info(list.size.toString())

        list.forEach { eod ->
            logger.info("saving $eod")
            endOfDayPriceDataRepository.save(
                EndOfDayPriceDataEntity(
                    ticker = eod.symbol,
                    date = eod.date,
                    price = eod.close,
                    currency = Currency.USD,
                ),
            )
        }
    }

    @PostConstruct
    fun test() {
        //        processEndOfDayData()
        //        logger.info(getTickerData("AAPL").toString())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

data class MarketStackEndOfDayPrices(
    val data: List<MarketStackEndOfDayData>,
    val pagination: MarketStackPagination,
)

data class MarketStackEndOfDayData(
    val close: BigDecimal,
    val symbol: String,
    val exchange: String,
    @JsonDeserialize(using = CustomLocalDateDeserializer::class) val date: LocalDate,
)

data class MarketStackPagination(
    val limit: Int,
    val offset: Int,
    val count: Int,
    val total: Int,
)

class CustomLocalDateDeserializer : JsonDeserializer<LocalDate>() {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(
        jsonParser: JsonParser,
        context: DeserializationContext?,
    ): LocalDate {
        val date: String = jsonParser.text
        return LocalDate.parse(date, formatter) // Only date part will be extracted
    }

    companion object {
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")
    }
}

data class MarketStackTicker(
    val name: String,
    val symbol: String,
    @JsonProperty("has_intraday") val hasIntraday: Boolean,
    @JsonProperty("has_eod") val hasEod: Boolean,
    val country: String?,
    @JsonProperty("stock_exchange") val stockExchange: MarketStackStockExchange,
)

data class MarketStackStockExchange(
    val name: String,
    val acronym: String,
    val mic: String,
    val country: String,
    @JsonProperty("country_code") val countryCode: String,
    val city: String,
    val website: String,
)

data class TickerData(
    val ticker: String,
    val currency: Currency,
)
