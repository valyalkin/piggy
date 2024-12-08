package com.valyalkin.market.eod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LatestPrice(
        String ticker,
        LocalDate latestDate,
        BigDecimal price
) {
}
