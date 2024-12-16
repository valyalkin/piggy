package com.valyalkin.market.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LatestPriceDto(
        String ticker,
        LocalDate latestDate,
        BigDecimal price
) {
}
