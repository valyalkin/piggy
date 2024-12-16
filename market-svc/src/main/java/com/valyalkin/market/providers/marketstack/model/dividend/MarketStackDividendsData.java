package com.valyalkin.market.providers.marketstack.model.dividend;

import com.valyalkin.market.providers.marketstack.model.MarketStackPagination;

import java.util.List;

public record MarketStackDividendsData(
    List<MarketStackDividend> data,
    MarketStackPagination pagination
) {
}
