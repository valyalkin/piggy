package com.valyalkin.market.providers.marketstack.model.dividend;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketStackDividend(
        LocalDate date, // Record date
        BigDecimal dividend,
        String symbol
) {
}
