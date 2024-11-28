package com.valyalkin.market.eod;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface EndOfDayPriceDataRepository extends JpaRepository<EndOfDayPriceDataEntity, UUID> {

    @Query(
            "SELECT MAX(e.date) " +
                    "FROM EndOfDayPriceDataEntity e " +
                    "WHERE e.ticker = :ticker"
    )
    LocalDate findLatestPriceDateForTicker(@Param("ticker") String ticker);

    @Query(
            "SELECT eod " +
                    "FROM EndOfDayPriceDataEntity eod " +
                    "WHERE eod.ticker = :ticker AND eod.date = (" +
                    "    SELECT MAX(eod1.date) " +
                    "    FROM EndOfDayPriceDataEntity eod1 " +
                    "    WHERE eod1.ticker = :ticker" +
                    ")"
    )
    EndOfDayPriceDataEntity findLatestPriceForTicker(@Param("ticker") String ticker);
}
