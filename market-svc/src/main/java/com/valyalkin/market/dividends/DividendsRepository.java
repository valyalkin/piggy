package com.valyalkin.market.dividends;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DividendsRepository extends JpaRepository<DividendsEntity, UUID> {

    @Query(
            "SELECT MAX(e.recordDate) " +
                    "FROM DividendsEntity e " +
                    "WHERE e.ticker = :ticker"
    )
    LocalDate findLatestDividendDateForTicker(@Param("ticker") String ticker);

    List<DividendsEntity> findByTickerOrderByRecordDateDesc(String ticker);
}
