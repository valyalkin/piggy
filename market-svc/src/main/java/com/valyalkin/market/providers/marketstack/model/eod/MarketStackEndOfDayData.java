package com.valyalkin.market.providers.marketstack.model.eod;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.valyalkin.market.providers.marketstack.config.CustomLocalDateDeserializer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarketStackEndOfDayData(
        BigDecimal close,
        String symbol,
        String exchange,
        @JsonDeserialize(using = CustomLocalDateDeserializer.class) LocalDate date
) {
}
