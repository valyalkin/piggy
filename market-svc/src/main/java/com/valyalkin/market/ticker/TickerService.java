package com.valyalkin.market.ticker;

import com.valyalkin.market.eod.Currency;
import com.valyalkin.market.providers.MarketDataProvider;
import com.valyalkin.market.providers.model.TickerDto;
import org.springframework.stereotype.Service;

@Service
public class TickerService {

    private final TickersRepository tickersRepository;
    private final MarketDataProvider marketDataProvider;

    public TickerService(TickersRepository tickersRepository, MarketDataProvider marketDataProvider) {
        this.tickersRepository = tickersRepository;
        this.marketDataProvider = marketDataProvider;
    }

    public TickerDto getTickerData(String ticker) {

        final var tickerEntity = tickersRepository.findBySymbol(ticker);

        if (tickerEntity != null) {
            return new TickerDto(
                    ticker,
                    tickerEntity.getName(),
                    tickerEntity.isHasEodPrice(),
                    tickerEntity.getExchange(),
                    tickerEntity.getAcronym(),
                    tickerEntity.getMic(),
                    tickerEntity.getCountry(),
                    tickerEntity.getCountryCode()
            );
        }

        final var tickerData = marketDataProvider.tickerData(ticker);

        final var entity = new TickersEntity();
        entity.setSymbol(tickerData.ticker());
        entity.setName(tickerData.name());
        entity.setHasEodPrice(tickerData.hasEodPrice());
        entity.setExchange(tickerData.exchange());
        entity.setAcronym(tickerData.acronym());
        entity.setMic(tickerData.mic());
        entity.setCountry(tickerData.country());
        entity.setCountryCode(tickerData.countryCode());
        entity.setCurrency(Currency.USD);

        tickersRepository.save(entity);

        return tickerData;

    }


}
