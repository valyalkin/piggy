package com.valyalkin.market.providers.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EndOfDayPrice(
        BigDecimal close,
        String ticker,
        String exchange,
        LocalDate date
) {
}
