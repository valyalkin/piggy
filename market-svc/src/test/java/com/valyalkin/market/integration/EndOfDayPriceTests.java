package com.valyalkin.market.integration;

import com.valyalkin.market.eod.EndOfDayDataService;
import com.valyalkin.market.eod.EndOfDayPriceDataRepository;
import com.valyalkin.market.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

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
    @Disabled
    void testInitialLoad() {

        var dividendsResponse = TestUtils.readFileFromResources(
                "marketstack/dividends/dividends-aapl-response-offset-0.json"
        );



        service.processDividends(ticker);

    }
}
