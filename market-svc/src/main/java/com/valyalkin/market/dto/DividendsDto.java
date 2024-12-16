package com.valyalkin.market.dto;

import java.util.List;

public record DividendsDto(
        String ticker,
        List<DividendDto> dividends
) {
}
