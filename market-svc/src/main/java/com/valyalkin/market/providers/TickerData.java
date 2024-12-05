package com.valyalkin.market.providers;

public record TickerData(
        String ticker,
        String name,
        boolean hasEodPrice,
        String exchange,
        String acronym,
        String mic,
        String country,
        String countryCode
) {
}
