package com.valyalkin.market.dividends;

import com.valyalkin.market.config.exception.NotFoundException;
import com.valyalkin.market.dto.DividendDto;
import com.valyalkin.market.dto.DividendsDto;
import com.valyalkin.market.providers.model.Dividend;
import com.valyalkin.market.providers.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DividendsService {

    private final DividendsRepository dividendsRepository;
    private final MarketDataProvider marketDataProvider;

    public DividendsService(DividendsRepository dividendsRepository, MarketDataProvider marketDataProvider) {
        this.dividendsRepository = dividendsRepository;
        this.marketDataProvider = marketDataProvider;
    }

    private @Value("${data.date-from}") LocalDate date;

    private static Logger logger = LoggerFactory.getLogger(DividendsService.class);

    public void processDividends(String ticker) {
        final var latestDate = dividendsRepository.findLatestDividendDateForTicker(ticker);

        final LocalDate dateFrom;

        if (latestDate == null) {
            dateFrom = date;
        } else {
            dateFrom = latestDate.plusDays(1);
        }

        final var dividendsPage = marketDataProvider.dividends(ticker, dateFrom, 0);

        final List<Dividend> list = new ArrayList<>(dividendsPage.dividends());

        final var pagination = dividendsPage.pagination();
        final int numCalls = (int) Math.ceil((double) pagination.total() / pagination.limit());

        if (numCalls != 0) {
            for (int i = 1; i <= numCalls; i++) {
                final var offset = i * pagination.limit();
                list.addAll(marketDataProvider.dividends(
                        ticker,
                        dateFrom,
                        offset
                ).dividends()
                );
            }
        }

        list.forEach(
                (dividend -> {
                    logger.info("saving " + dividend);
                    final var entity = new DividendsEntity();
                    entity.setDividend(dividend.dividend());
                    entity.setDate(dividend.date());
                    entity.setTicker(dividend.ticker());
                    dividendsRepository.save(entity);
                })
        );
    }

    public DividendsDto dividends(String ticker) {

        final List<DividendsEntity> entities = dividendsRepository.findByTickerOrderByDateDesc(ticker);

        if (entities != null) {
            var dividends = entities.stream().map(
                    (dividendsEntity ->
                        new DividendDto(
                                dividendsEntity.getDate(),
                                dividendsEntity.getDividend()
                        )
                    )
            ).toList();

            return new DividendsDto(
                    ticker,
                    dividends
            );
        } else {
            throw new NotFoundException(
                    String.format("No dividends founds for ticker %s", ticker)
            );
        }

    }
}
