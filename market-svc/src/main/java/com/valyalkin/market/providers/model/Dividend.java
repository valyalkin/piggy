package com.valyalkin.market.providers.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Dividend(
        LocalDate date,
        BigDecimal dividend,
        String ticker
) {
}
