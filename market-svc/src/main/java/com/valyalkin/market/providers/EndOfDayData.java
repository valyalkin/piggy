package com.valyalkin.market.providers;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EndOfDayData(
        BigDecimal close,
        String ticker,
        String exchange,
        LocalDate date
) {
}
