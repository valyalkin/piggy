package com.valyalkin.portfolio.stocks.holdings

import com.valyalkin.portfolio.stocks.transactions.Currency
import jakarta.persistence.*
import org.springframework.data.repository.CrudRepository
import java.math.BigDecimal
import java.util.*

interface StockHoldingsRepository : CrudRepository<StockHoldingEntity, UUID> {
    fun getByUserIdAndTicker(
        userId: String,
        ticker: String,
    ): List<StockHoldingEntity>

    fun getAllByUserIdAndCurrency(
        userId: String,
        currency: Currency,
    ): List<StockHoldingEntity>

    fun getByUserId(userId: String): List<StockHoldingEntity>

    fun deleteByUserIdAndTicker(
        userId: String,
        ticker: String,
    )
}

@Entity
@Table(name = "stock_holdings")
data class StockHoldingEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID) val id: UUID = UUID.randomUUID(),
    @Column(name = "user_id") val userId: String,
    @Column(name = "ticker") val ticker: String,
    val quantity: Long,
    val averagePrice: BigDecimal,
    @Enumerated(EnumType.STRING) val currency: Currency,
)
