package com.valyalkin.market.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividendDto(
        LocalDate date,
        BigDecimal dividend
) {
}
