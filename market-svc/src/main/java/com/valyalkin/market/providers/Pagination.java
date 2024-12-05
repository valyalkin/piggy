package com.valyalkin.market.providers;

public record Pagination(
        int limit,
        int offset,
        int count,
        int total
) {
}
