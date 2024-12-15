package com.valyalkin.market.integration;

import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.valyalkin.market.ticker.TickersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@ActiveProfiles({"test", "wiremock"})
@AutoConfigureMockMvc
@EnableWireMock
public class TicketDataTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TickersRepository tickersRepository;
    @Autowired
    private RestClient restClient;

    @Value("${wiremock.server.port}")
    private String wiremockPort;

    @BeforeEach
    public void cleanUp() {
        tickersRepository.deleteAll();
    }

    @Test
    @DisplayName("Ticker Data - get from source")
    void testTickerDataFromSource() throws Exception {

        String response = """
                {
                  "name": "Example Company",
                  "symbol": "EXMPL",
                  "has_intraday": true,
                  "has_eod": true,
                  "country": "USA",
                  "stock_exchange": {
                    "name": "New York Stock Exchange",
                    "acronym": "NYSE",
                    "country": "USA",
                    "city": "New York",
                    "website": "https://www.nyse.com"
                  }
                }
                """;

        stubFor(
                get(
                        urlPathMatching("/v1/tickers/AAPL"))
                        .withQueryParam("access_key", equalTo("test"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(response)
                )
        );

        var tickerData = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/ticker/AAPL")
        ).andExpect(
                MockMvcResultMatchers.status().is2xxSuccessful()
        ).andReturn().getResponse().getContentAsString();



    }
}
