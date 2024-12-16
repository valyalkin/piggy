package com.valyalkin.market.eod;

import com.valyalkin.market.config.exception.NotFoundException;
import com.valyalkin.market.dto.LatestPriceDto;
import com.valyalkin.market.providers.model.EndOfDayPrice;
import com.valyalkin.market.providers.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class EndOfDayDataService {

    private final EndOfDayPriceDataRepository endOfDayPriceDataRepository;
    private final MarketDataProvider marketDataProvider;

    public EndOfDayDataService(EndOfDayPriceDataRepository endOfDayPriceDataRepository, MarketDataProvider marketDataProvider) {
        this.endOfDayPriceDataRepository = endOfDayPriceDataRepository;
        this.marketDataProvider = marketDataProvider;
    }

    private static Logger logger = LoggerFactory.getLogger(EndOfDayDataService.class);

    public void processDividends(String ticker) {

        final var latestDate = endOfDayPriceDataRepository.findLatestPriceDateForTicker(ticker);

        final LocalDate dateFrom;

        if (latestDate == null) {
            dateFrom = LocalDate.of(2019, 1, 1); // TODO:  Have it as configuration
        } else {
            dateFrom = latestDate.plusDays(1);
        }

        final var eodDate = marketDataProvider.endOfDayData(ticker, dateFrom, 0);

        final List<EndOfDayPrice> list = new ArrayList<>(eodDate.eod());

        final var pagination = eodDate.pagination();
        final int numCalls = (int) Math.ceil((double) pagination.total() / pagination.limit());

        if (numCalls != 0) {
            for (int i = 1; i <= numCalls; i++) {
                final var offset = i * pagination.limit();
                final var eod = marketDataProvider.endOfDayData(ticker, dateFrom, offset);
                list.addAll(eod.eod());
            }
        }

        list.forEach(
                (eod) -> {
                    logger.info("saving " + eod);
                    final var entity = new EndOfDayPriceDataEntity();
                    entity.setCurrency(Currency.USD);
                    entity.setDate(eod.date());
                    entity.setPrice(eod.close());
                    entity.setTicker(ticker);
                    endOfDayPriceDataRepository.save(entity);
                }
        );

    }

    public LatestPriceDto latestPriceForTicker(String ticker) {
        final var entity = endOfDayPriceDataRepository.findLatestPriceForTicker(ticker);

        if (entity != null) {
            return new LatestPriceDto(
                    ticker,
                    entity.getDate(),
                    entity.getPrice()
            );
        } else {
            throw new NotFoundException(String.format("No price found for ticker %s", ticker));
        }
    }

}
