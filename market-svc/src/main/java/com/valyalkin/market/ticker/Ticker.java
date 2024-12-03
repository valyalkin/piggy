package com.valyalkin.market.ticker;

import com.valyalkin.market.eod.Currency;

public record Ticker(
        String ticker,
        Currency currency
) {
}
