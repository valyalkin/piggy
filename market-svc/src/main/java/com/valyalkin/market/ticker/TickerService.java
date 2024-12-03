package com.valyalkin.market.ticker;

import com.valyalkin.market.providers.marketstack.ticker.MarketStackTicker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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

    private MarketStackTicker getTickerData(String ticker) {

        String uri = UriComponentsBuilder
                .fromUriString(marketStackUrl + "/v1/tickers/" + ticker)
                .queryParam("access_key", marketStackApiKey)
                .toUriString();







    }


}
