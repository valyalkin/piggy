package com.valyalkin.market.providers.marketstack.ticker;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarketStackStockExchange(
        String name,
        String acronym,
        String mic,
        String country,
        @JsonProperty("country_code") String countryCode,
        String city,
        String website
) {}
