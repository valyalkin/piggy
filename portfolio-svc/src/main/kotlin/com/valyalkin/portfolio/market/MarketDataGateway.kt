package com.valyalkin.portfolio.market

import com.valyalkin.portfolio.configuration.exception.SystemException
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate

@Service
class MarketDataGateway(
    private val restClient: RestClient,
    @Value("\${dependencies.market.url}") private val marketSvcUrl: String,
) {
    private val tickerDataPath = "v1/ticker"
    private val latestPricePath = "v1/eod/latest-price"

    fun tickerData(ticker: String): TickerData {
        val url =
            UriComponentsBuilder
                .fromUriString("$marketSvcUrl/$tickerDataPath/$ticker")
                .toUriString()

        return restClient
            .get()
            .uri(url)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, response ->
                throw SystemException(
                    "Market service api call failed with error ${response.statusCode.value()}," +
                        " Details: ${response.body.readAllBytes().toString(Charset.defaultCharset())}",
                )
            }.body(TickerData::class.java) ?: throw SystemException("Not able to fetch the data from market svc")
    }

    fun latestPrice(ticker: String): LatestPrice {
        val url =
            UriComponentsBuilder
                .fromUriString("$marketSvcUrl/$latestPricePath/$ticker")
                .toUriString()

        return restClient
            .get()
            .uri(url)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, response ->
                throw SystemException(
                    "Market service api call failed with error ${response.statusCode.value()}," +
                        " Details: ${response.body.readAllBytes().toString(Charset.defaultCharset())}",
                )
            }.body(LatestPrice::class.java) ?: throw SystemException("Not able to fetch the data from market svc")
    }
}

data class TickerData(
    val ticker: String,
    val name: String,
    val hasEodPrice: Boolean,
    val exchange: String,
    val acronym: String,
    val mic: String,
    val country: String,
    val countryCode: String,
)

data class LatestPrice(
    val ticker: String,
    val latestDate: LocalDate,
    val price: BigDecimal,
)
