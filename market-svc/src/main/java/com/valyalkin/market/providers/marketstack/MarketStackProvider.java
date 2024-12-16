package com.valyalkin.market.providers.marketstack;

import com.valyalkin.market.config.exception.SystemException;
import com.valyalkin.market.providers.*;
import com.valyalkin.market.providers.marketstack.model.dividend.MarketStackDividendsData;
import com.valyalkin.market.providers.marketstack.model.eod.MarketStackEndOfDayPrices;
import com.valyalkin.market.providers.marketstack.model.ticker.MarketStackTicker;
import com.valyalkin.market.providers.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Component
public class MarketStackProvider implements MarketDataProvider {

    private final RestClient client;
    @Value("${data.marketstack.url}")
    private String marketStackUrl;
    @Value("${data.marketstack.apikey}")
    private String marketStackApiKey;

    public MarketStackProvider(RestClient client) {
        this.client = client;
    }


    @Override
    public TickerDto tickerData(String ticker) {
        final String uri = UriComponentsBuilder
                .fromUriString(marketStackUrl + "/v1/tickers/" + ticker)
                .queryParam("access_key", marketStackApiKey)
                .toUriString();

        final var marketStackTicker = client.get()
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

        if (marketStackTicker != null) {
            return new TickerDto(
                    marketStackTicker.symbol(),
                    marketStackTicker.name(),
                    marketStackTicker.hasEod(),
                    marketStackTicker.stockExchange().name(),
                    marketStackTicker.stockExchange().acronym(),
                    marketStackTicker.stockExchange().mic(),
                    marketStackTicker.stockExchange().country(),
                    marketStackTicker.stockExchange().countryCode()
            );
        } else {
            throw new SystemException("Not able to fetch data from marketstack");
        }
    }

    @Override
    public EndOfDatePage endOfDayData(String tickers, LocalDate dateFrom, int offset) {
        String uri = UriComponentsBuilder
                .fromUriString(marketStackUrl + "/v1/eod")
                .queryParam("access_key", marketStackApiKey)
                .queryParam("symbols", tickers)
                .queryParam("date_from", dateFrom.toString())
                .queryParam("limit", 100)
                .queryParam("offset", offset)
                .toUriString();

        var result = client.get()
                .uri(uri)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        ((request, response) -> {
                            var statusCode = response.getStatusCode().value();
                            var details = new String(
                                    response.getBody().readAllBytes(),
                                    StandardCharsets.UTF_8
                            );
                            throw new SystemException(
                                    String.format(
                                            "Marketstack api call failed with error %d Details: %s",
                                            statusCode,
                                            details
                                    )
                            );
                        })

                ).body(MarketStackEndOfDayPrices.class);

        if (result != null) {
            return new EndOfDatePage(
                    result.data().stream().map(
                            (marketStackEndOfDayData -> new EndOfDayPrice(
                                    marketStackEndOfDayData.close(),
                                    marketStackEndOfDayData.symbol(),
                                    marketStackEndOfDayData.exchange(),
                                    marketStackEndOfDayData.date()
                            ))
                    ).toList(),
                    new Pagination(
                            result.pagination().limit(),
                            result.pagination().offset(),
                            result.pagination().count(),
                            result.pagination().total()
                    )
            );
        } else {
            throw new SystemException("Not able to fetch the data from marketstack");
        }
    }

    @Override
    public DividendsPage dividends(String ticker, LocalDate dateFrom, int offset) {
        String uri = UriComponentsBuilder
                .fromUriString(marketStackUrl + "/v1/dividends")
                .queryParam("access_key", marketStackApiKey)
                .queryParam("symbols", ticker)
                .queryParam("date_from", dateFrom.toString())
                .queryParam("limit", 1000)
                .queryParam("offset", offset)
                .toUriString();

        var result = client.get()
                .uri(uri)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        ((request, response) -> {
                            var statusCode = response.getStatusCode().value();
                            var details = new String(
                                    response.getBody().readAllBytes(),
                                    StandardCharsets.UTF_8
                            );
                            throw new SystemException(
                                    String.format(
                                            "Marketstack api call failed with error %d Details: %s",
                                            statusCode,
                                            details
                                    )
                            );
                        })

                ).body(MarketStackDividendsData.class);

        if (result != null) {
            return new DividendsPage(
                    result.data().stream().map(
                            (dividend -> new Dividend(
                                    dividend.date(),
                                    dividend.dividend(),
                                    dividend.symbol()
                            ))
                    ).toList(),
                    new Pagination(
                            result.pagination().limit(),
                            result.pagination().offset(),
                            result.pagination().count(),
                            result.pagination().total()
                    )
            );
        } else {
            throw new SystemException("Not able to fetch the data from marketstack");
        }
    }
}
