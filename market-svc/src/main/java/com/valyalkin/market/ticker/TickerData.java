package com.valyalkin.market.ticker;

import com.valyalkin.market.eod.Currency;

public record TickerData(
        String ticker,
        Currency currency,
        String name,
        boolean hasEodPrice,
        String exchange,
        String acronym,
        String mic,
        String country,
        Country countryCode
) {
}
