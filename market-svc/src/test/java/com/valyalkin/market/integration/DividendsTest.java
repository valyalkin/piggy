package com.valyalkin.market.integration;

import com.valyalkin.market.dividends.DividendsEntity;
import com.valyalkin.market.dividends.DividendsRepository;
import com.valyalkin.market.dividends.DividendsService;
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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "wiremock"})
@EnableWireMock
public class DividendsTest {

    @Autowired
    private DividendsRepository dividendsRepository;

    @Autowired
    private DividendsService service;

    @BeforeEach
    public void cleanUp() {
        dividendsRepository.deleteAll();
    }

    private String ticker = "AAPL";

    @Test
    @DisplayName("Initial load - no data set for the ticker")
    void testInitialLoad() {

        final var symbol = "AAPL";

        var dividendsResponseOffset0 = TestUtils.readFileFromResources(
                "marketstack/dividends/dividends-aapl-response-offset-0.json"
        );

        var dividendsResponseOffset1000 = TestUtils.readFileFromResources(
                "marketstack/dividends/dividends-aapl-response-offset-1000.json"
        );

        stubFor(
                get(
                        urlPathMatching("/v1/dividends"))
                        .withQueryParam("access_key", equalTo("test"))
                        .withQueryParam("symbols", equalTo(symbol))
                        .withQueryParam("offset", equalTo("0"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(dividendsResponseOffset0)
                        )
        );

        stubFor(
                get(
                        urlPathMatching("/v1/dividends"))
                        .withQueryParam("access_key", equalTo("test"))
                        .withQueryParam("symbols", equalTo(symbol))
                        .withQueryParam("offset", equalTo("1000"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(dividendsResponseOffset1000)
                        )
        );

        service.processDividends(ticker);

        var entities = dividendsRepository.findByTickerOrderByRecordDateDesc(symbol);
        assertThat(entities.size()).isEqualTo(86);

    }

    @Test
    @DisplayName("Addition - should add to previously saved dividends for this ticker")
    void testAddition() {

        final var symbol = "AAPL";

        var dividendsResponse = TestUtils.readFileFromResources(
                "marketstack/dividends/dividends-aapl-response-offset-0-newData.json"
        );

        var dividendsResponseOffset1000 = TestUtils.readFileFromResources(
                "marketstack/dividends/dividends-aapl-response-offset-1000.json"
        );



        stubFor(
                get(
                        urlPathMatching("/v1/dividends"))
                        .withQueryParam("access_key", equalTo("test"))
                        .withQueryParam("symbols", equalTo(symbol))
                        .withQueryParam("offset", equalTo("0"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(dividendsResponse)
                        )
        );

        stubFor(
                get(
                        urlPathMatching("/v1/dividends"))
                        .withQueryParam("access_key", equalTo("test"))
                        .withQueryParam("symbols", equalTo(symbol))
                        .withQueryParam("offset", equalTo("1000"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(dividendsResponseOffset1000)
                        )
        );

        // Previously saved dividend
        final var entity = new DividendsEntity();
        entity.setDividend(BigDecimal.valueOf(0.25));
        entity.setRecordDate(LocalDate.of(2024, 8, 12));
        entity.setTicker(symbol);
        dividendsRepository.save(entity);

        service.processDividends(ticker);

        var entities = dividendsRepository.findByTickerOrderByRecordDateDesc(symbol);
        assertThat(entities.size()).isEqualTo(2);

        assertThat(entities.getFirst().getTicker()).isEqualTo(symbol);
        assertThat(entities.getFirst().getRecordDate()).isEqualTo(LocalDate.of(2024, 11, 8));
        assertThat(entities.getFirst().getDividend()).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.25));

        assertThat(entities.get(1).getTicker()).isEqualTo(symbol);
        assertThat(entities.get(1).getRecordDate()).isEqualTo(LocalDate.of(2024, 8, 12));
        assertThat(entities.get(1).getDividend()).isGreaterThanOrEqualTo(BigDecimal.valueOf(0.25));





    }
}
