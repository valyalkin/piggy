package com.valyalkin.market.providers.marketstack.eod;

import com.valyalkin.market.providers.marketstack.MarketStackPagination;

import java.util.List;

public record MarketStackEndOfDayPrices(
        List<MarketStackEndOfDayData> data, MarketStackPagination pagination
) {}
