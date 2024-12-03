package com.valyalkin.market.providers.marketstack.ticker;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MarketStackTicker(
        String name,
        String symbol,
        @JsonProperty("has_intraday") boolean hasIntraday,
        @JsonProperty("has_eod") boolean hasEod,
        String country,
        @JsonProperty("stock_exchange") MarketStackStockExchange stockExchange
) {}
