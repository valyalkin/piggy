package com.valyalkin.market.providers;

import java.time.LocalDate;

public interface MarketDataProvider {
    TickerData tickerData(String ticker);
    EndOfDateDataPage endOfDayData(String tickers, LocalDate dateFrom, int offset);
}
