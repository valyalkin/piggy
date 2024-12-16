package com.valyalkin.market.providers.model;

public record TickerDto(
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
