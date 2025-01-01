package com.valyalkin.market.integration;

import com.valyalkin.market.eod.Currency;
import com.valyalkin.market.eod.EndOfDayDataService;
import com.valyalkin.market.eod.EndOfDayPriceDataEntity;
import com.valyalkin.market.eod.EndOfDayPriceDataRepository;
import com.valyalkin.market.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "wiremock"})
@EnableWireMock
public class EndOfDayPriceTests {

    @Autowired
    private EndOfDayPriceDataRepository endOfDayPriceDataRepository;

    @Autowired
    private EndOfDayDataService service;

    @BeforeEach
    public void cleanUp() {
        endOfDayPriceDataRepository.deleteAll();
    }

    private String ticker = "AAPL";

    @Test
    @DisplayName("Initial load - no data set for the ticker")
    void testInitialLoad() {

        var eodResponseOffset0 = TestUtils.readFileFromResources(
                "marketstack/eod/eod-appl-offset-0.json"
        );

        var eodResponseOffset100 = TestUtils.readFileFromResources(
                "marketstack/eod/eod-appl-offset-100.json"
        );

        var eodResponseOffset200 = TestUtils.readFileFromResources(
                "marketstack/eod/eod-appl-offset-200.json"
        );

        var eodResponseOffset300 = TestUtils.readFileFromResources(
                "marketstack/eod/eod-appl-offset-300.json"
        );

        final Map<String, String> offsetToResponseMap = Map.of(
                "0", eodResponseOffset0,
                "100", eodResponseOffset100,
                "200", eodResponseOffset200,
                "300", eodResponseOffset300
        );

        offsetToResponseMap.forEach(
                (offset, response) -> {
                    stubFor(
                            get(
                                    urlPathMatching("/v1/eod"))
                                    .withQueryParam("access_key", equalTo("test"))
                                    .withQueryParam("symbols", equalTo(ticker))
                                    .withQueryParam("offset", equalTo(offset))
                                    .willReturn(
                                            aResponse()
                                                    .withHeader("Content-Type", "application/json")
                                                    .withStatus(200)
                                                    .withBody(response)
                                    )
                    );
                }
        );

        service.processEodData(ticker);


        List<EndOfDayPriceDataEntity> entities = endOfDayPriceDataRepository.findByTicker(ticker);

        assertThat(entities.size()).isEqualTo(251);

    }

    @Test
    @DisplayName("Append existing data")
    void testAppendExistingData() {

        LocalDate lastDate = LocalDate.of(2024, 12, 11);

        // Populate the data, such that last date was 2024-12-11
        EndOfDayPriceDataEntity lastEntity = new EndOfDayPriceDataEntity();
        lastEntity.setTicker(ticker);
        lastEntity.setDate(lastDate);
        lastEntity.setPrice(BigDecimal.valueOf(246.49));
        lastEntity.setCurrency(Currency.USD);

        endOfDayPriceDataRepository.save(lastEntity);

        var eodResponseOffset0 = TestUtils.readFileFromResources(
                "marketstack/eod/eod-appl-offset-0-newDate.json"
        );

        var eodResponseOffset100 = TestUtils.readFileFromResources(
                "marketstack/eod/eod-appl-offset-100-newDate.json"
        );

        Map.of(
                "0", eodResponseOffset0,
                "100", eodResponseOffset100
        ).forEach(
                (offset, response) -> {
                    stubFor(
                            get(
                                    urlPathMatching("/v1/eod"))
                                    .withQueryParam("access_key", equalTo("test"))
                                    .withQueryParam("symbols", equalTo(ticker))
                                    .withQueryParam("offset", equalTo(offset))
                                    .withQueryParam("date_from", equalTo(lastDate.plusDays(1).toString()))
                                    .willReturn(
                                            aResponse()
                                                    .withHeader("Content-Type", "application/json")
                                                    .withStatus(200)
                                                    .withBody(response)
                                    )
                    );
                }
        );

        service.processEodData(ticker);

        List<EndOfDayPriceDataEntity> entities = endOfDayPriceDataRepository.findByTicker(ticker);

        assertThat(entities.size()).isEqualTo(4);

        var dates  = entities.stream()
                .map(EndOfDayPriceDataEntity::getDate)
                .sorted()
                .toList();


    }
}
