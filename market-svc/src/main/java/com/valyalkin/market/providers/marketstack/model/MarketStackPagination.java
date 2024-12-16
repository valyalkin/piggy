package com.valyalkin.market.providers.marketstack.model;

public record MarketStackPagination(
        int limit,
        int offset,
        int count,
        int total
) {
}
