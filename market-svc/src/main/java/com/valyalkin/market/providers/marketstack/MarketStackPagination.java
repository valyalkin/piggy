package com.valyalkin.market.providers.marketstack;

public record MarketStackPagination(
        int limit,
        int offset,
        int count,
        int total
) {
}
