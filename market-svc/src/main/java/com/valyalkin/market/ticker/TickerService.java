package com.valyalkin.market.ticker;

import com.valyalkin.market.eod.Currency;
import com.valyalkin.market.exception.BusinessException;
import com.valyalkin.market.exception.SystemException;
import com.valyalkin.market.providers.marketstack.ticker.MarketStackTicker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class TickerService {

    private TickersRepository tickersRepository;

    public TickerService(TickersRepository tickersRepository) {
        this.tickersRepository = tickersRepository;
    }

    private RestClient client = RestClient.builder().build();
    @Value("${data.marketstack.url}")
    private String marketStackUrl;
    @Value("${data.marketstack.apikey}")
    private String marketStackApiKey;

    private Map<Country, Currency> countryToCurrencyMap = Map.of(
            Country.US, Currency.USD,
            Country.SG, Currency.SGD
    );


    private MarketStackTicker fetchTickerData(String ticker) {

        final String uri = UriComponentsBuilder
                .fromUriString(marketStackUrl + "/v1/tickers/" + ticker)
                .queryParam("access_key", marketStackApiKey)
                .toUriString();

        final var result = client.get()
                .uri(uri)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        ((request, response) -> {
                            var statusCode = response.getStatusCode().value();
                            var details = new String(
                                    response.getBody().readAllBytes(), StandardCharsets.UTF_8
                            );
                            throw new SystemException(
                                    String.format(
                                            "Marketstack api call failed with error %d Details: %s",
                                            statusCode,
                                            details
                                    )
                            );
                        })

                ).body(MarketStackTicker.class);

        if (result != null) {
            return result;
        } else {
            throw new SystemException("Not able to fetch data from marketstack");
        }
    }

    public TickerData getTickerData(String ticker) {

        final var tickerEntity = tickersRepository.findBySymbol(ticker);

        if (tickerEntity != null) {
            return new TickerData(
                    ticker,
                    tickerEntity.getCurrency(),
                    tickerEntity.getName(),
                    tickerEntity.isHasEodPrice(),
                    tickerEntity.getExchange(),
                    tickerEntity.getAcronym(),
                    tickerEntity.getMic(),
                    tickerEntity.getCountry(),
                    tickerEntity.getCountryCode()
            );
        }

        final var marketStackTicker = fetchTickerData(ticker);

        final Country countryEnum;

        try {
            countryEnum = Country.valueOf(marketStackTicker.stockExchange().countryCode().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    String.format("Country %s is currently not supported",
                            marketStackTicker.stockExchange().countryCode()
                    )
            );
        }

        final Currency currency = countryToCurrencyMap.get(countryEnum);
        if (currency == null) {
            throw new BusinessException(
                    String.format(
                            "Currency for country %s is currently not supported",
                            countryEnum.name()
                    )
            );
        }

        final var entity = new TickersEntity();
        entity.setSymbol(ticker);
        entity.setCurrency(currency);
        entity.setName(marketStackTicker.name());
        entity.setHasEodPrice(marketStackTicker.hasEod());
        entity.setExchange(marketStackTicker.stockExchange().name());
        entity.setAcronym(marketStackTicker.stockExchange().acronym());
        entity.setMic(marketStackTicker.stockExchange().mic());
        entity.setCountry(marketStackTicker.stockExchange().country());
        entity.setCountryCode(countryEnum);

        tickersRepository.save(entity);

        return new TickerData(
                ticker,
                entity.getCurrency(),
                entity.getName(),
                entity.isHasEodPrice(),
                entity.getExchange(),
                entity.getAcronym(),
                entity.getMic(),
                entity.getCountry(),
                entity.getCountryCode()
        );

    }


}
