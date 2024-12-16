package com.valyalkin.market.providers;

import com.valyalkin.market.providers.model.DividendsPage;
import com.valyalkin.market.providers.model.EndOfDatePage;
import com.valyalkin.market.providers.model.TickerDto;

import java.time.LocalDate;

public interface MarketDataProvider {
    TickerDto tickerData(String ticker);
    EndOfDatePage endOfDayData(String tickers, LocalDate dateFrom, int offset);
    DividendsPage dividends(String ticker, LocalDate dateFrom, int offset);
}
