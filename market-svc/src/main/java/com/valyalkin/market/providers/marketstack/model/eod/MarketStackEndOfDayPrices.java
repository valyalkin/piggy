package com.valyalkin.market.providers.marketstack.model.eod;

import com.valyalkin.market.providers.marketstack.model.MarketStackPagination;

import java.util.List;

public record MarketStackEndOfDayPrices(
        List<MarketStackEndOfDayData> data, MarketStackPagination pagination
) {}
