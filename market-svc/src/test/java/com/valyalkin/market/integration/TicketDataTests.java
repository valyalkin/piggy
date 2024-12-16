package com.valyalkin.market.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.valyalkin.market.eod.Currency;
import com.valyalkin.market.providers.model.TickerDto;
import com.valyalkin.market.ticker.TickersEntity;
import com.valyalkin.market.ticker.TickersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "wiremock"})
@AutoConfigureMockMvc
@EnableWireMock
public class TicketDataTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TickersRepository tickersRepository;


    @BeforeEach
    public void cleanUp() {
        tickersRepository.deleteAll();
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Get from the provider")
    void testTickerDataFromDataProvider() throws Exception {

        String symbol = "AAPL";

        String response = """
                {
                  "name": "Apple Inc",
                  "symbol": "AAPL",
                  "has_intraday": false,
                  "has_eod": true,
                  "country": null,
                  "stock_exchange": {
                    "name": "NASDAQ Stock Exchange",
                    "acronym": "NASDAQ",
                    "mic": "XNAS",
                    "country": "USA",
                    "country_code": "US",
                    "city": "New York",
                    "website": "WWW.NASDAQ.COM"
                  }
                }
                """;

        stubFor(
                get(
                        urlPathMatching("/v1/tickers/" + symbol))
                        .withQueryParam("access_key", equalTo("test"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withStatus(200)
                                        .withBody(response)
                )
        );

        var tickerData = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/ticker/" + symbol)
        ).andExpect(
                MockMvcResultMatchers.status().is2xxSuccessful()
        ).andReturn().getResponse().getContentAsString();

        TickerDto tickerDto = objectMapper.readValue(tickerData, TickerDto.class);

        // TODO: Provide the correct data and verify not null values
        assertThat(tickerDto.ticker()).isEqualTo(symbol);
        assertThat(tickerDto.name()).isEqualTo("Apple Inc");
        assertThat(tickerDto.hasEodPrice()).isTrue();
        assertThat(tickerDto.exchange()).isEqualTo("NASDAQ Stock Exchange");
        assertThat(tickerDto.acronym()).isEqualTo("NASDAQ");
        assertThat(tickerDto.country()).isEqualTo("USA");
        assertThat(tickerDto.countryCode()).isEqualTo("US");

        var entity = tickersRepository.findBySymbol("AAPL");

        assertThat(entity).isNotNull();
        assertThat(entity.getSymbol()).isEqualTo(symbol);
        assertThat(entity.getName()).isEqualTo("Apple Inc");
        assertThat(entity.isHasEodPrice()).isTrue();
        assertThat(entity.getExchange()).isEqualTo("NASDAQ Stock Exchange");
        assertThat(entity.getAcronym()).isEqualTo("NASDAQ");
        assertThat(entity.getCountry()).isEqualTo("USA");
        assertThat(entity.getCountryCode()).isEqualTo("US");
    }

    @Test
    @DisplayName("Get previously saved data")
    void testTickerDataFromDatabase() throws Exception {
        var ticker = "ticker";
        var name = "name";
        var hasEodPrice = false;
        var exchange = "exchange";
        var acronym = "acronym";
        var mic = "mic";
        var country = "USA";
        var currency = Currency.USD;
        var countryCode = "US";

        final var entity = new TickersEntity();
        entity.setSymbol(ticker);
        entity.setName(name);
        entity.setHasEodPrice(hasEodPrice);
        entity.setExchange(exchange);
        entity.setAcronym(acronym);
        entity.setMic(mic);
        entity.setCountry(country);
        entity.setCountryCode(countryCode);
        entity.setCurrency(currency);

        tickersRepository.save(entity);

        var tickerData = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/ticker/" + ticker)
        ).andExpect(
                MockMvcResultMatchers.status().is2xxSuccessful()
        ).andReturn().getResponse().getContentAsString();

        TickerDto t = objectMapper.readValue(tickerData, TickerDto.class);

        assertThat(t.ticker()).isEqualTo(ticker);
        assertThat(t.name()).isEqualTo(name);
        assertThat(t.hasEodPrice()).isFalse();
        assertThat(t.exchange()).isEqualTo(exchange);
        assertThat(t.acronym()).isEqualTo(acronym);
        assertThat(t.country()).isEqualTo(country);
        assertThat(t.countryCode()).isEqualTo(countryCode);

    }
}
