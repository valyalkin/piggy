package com.valyalkin.portfolio.stocks.holdings

import com.valyalkin.portfolio.stocks.transactions.Currency
import jakarta.persistence.*
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

interface HistoricalStockHoldingsRepository : CrudRepository<HistoricalStockHoldingEntity, UUID> {
    fun deleteByUserIdAndTicker(
        userId: String,
        ticker: String,
    )

    fun findByUserIdAndTicker(
        userId: String,
        ticker: String,
    ): List<HistoricalStockHoldingEntity>

    fun findByUserId(userId: String): List<HistoricalStockHoldingEntity>
}

@Entity
@Table(name = "historical_stock_holdings")
data class HistoricalStockHoldingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id") val userId: String,
    @Column(name = "ticker") val ticker: String,
    val quantity: Long,
    val averagePrice: BigDecimal,
    @Enumerated(EnumType.STRING) val currency: Currency,
    @Column(name = "start_date") @Temporal(TemporalType.DATE) val startDate: LocalDate,
    @Column(name = "end_date", nullable = true) @Temporal(TemporalType.DATE) val endDate: LocalDate?,
)
