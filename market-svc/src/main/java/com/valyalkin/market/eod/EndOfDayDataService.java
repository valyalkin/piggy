package com.valyalkin.market.eod;

import com.valyalkin.market.exception.SystemException;
import com.valyalkin.market.providers.marketstack.eod.MarketStackEndOfDayData;
import com.valyalkin.market.providers.marketstack.eod.MarketStackEndOfDayPrices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EndOfDayDataService {

    private final EndOfDayPriceDataRepository endOfDayPriceDataRepository;
    private RestClient client = RestClient.builder().build();
    @Value("${data.marketstack.url}")
    private String marketStackUrl;
    @Value("${data.marketstack.apikey}")
    private String marketStackApiKey;

    public EndOfDayDataService(EndOfDayPriceDataRepository endOfDayPriceDataRepository) {
        this.endOfDayPriceDataRepository = endOfDayPriceDataRepository;
    }

    private static Logger logger = LoggerFactory.getLogger(EndOfDayDataService.class);


    private MarketStackEndOfDayPrices fetchEndOfDayPrices(
            LocalDate dateFrom,
            int offset,
            String symbols
    ) {
        String uri = UriComponentsBuilder
                .fromUriString(marketStackUrl + "/v1/eod")
                .queryParam("access_key", marketStackApiKey)
                .queryParam("symbols", symbols)
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

                ).body(MarketStackEndOfDayPrices.class);

        if (result != null) {
            return result;
        } else {
            throw new SystemException("Not able to fetch the data from marketstack");
        }
    }

    public void processEndOfDayData(String ticker) {

        final var latestDate = endOfDayPriceDataRepository.findLatestPriceDateForTicker(ticker);

        final LocalDate dateFrom;

        if (latestDate == null) {
            dateFrom = LocalDate.of(2019, 1, 1); // TODO:  Have it as configuration
        } else {
            dateFrom = latestDate.plusDays(1);
        }

        final var result = fetchEndOfDayPrices(
                dateFrom,
                0,
                ticker
        );

        final List<MarketStackEndOfDayData> list = new ArrayList<>(result.data());

        final var pagination = result.pagination();
        final int numCalls = (int) Math.ceil((double) pagination.total() / pagination.limit());

        if (numCalls != 0) {
            for (int i = 1; i <= numCalls; i++) {
                final var offset = i * pagination.limit();
                final var eod = fetchEndOfDayPrices(dateFrom, offset, ticker);
                list.addAll(eod.data());

            }
        }

        list.forEach(
                (eod) -> {
                    logger.info("saving " + eod);
                    final var entity = new EndOfDayPriceDataEntity();
                    entity.setCurrency(Currency.USD);
                    entity.setDate(eod.date());
                    entity.setPrice(eod.close());
                    entity.setTicker(ticker);
                    endOfDayPriceDataRepository.save(entity);
                }
        );

    }

}
