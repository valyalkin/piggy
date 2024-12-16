package com.valyalkin.market.providers.model;

public record Pagination(
        int limit,
        int offset,
        int count,
        int total
) {
}
