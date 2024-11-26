package com.valyalkin.portfolio.stocks.holdings

import com.valyalkin.portfolio.stocks.transactions.Currency
import org.springframework.stereotype.Service

@Service
class StockHoldingsService(
    private val stockHoldingsRepository: StockHoldingsRepository,
) {
    fun stockHoldings(
        userId: String,
        currency: Currency,
    ): List<StockHoldingEntity> = stockHoldingsRepository.getAllByUserIdAndCurrency(userId, currency)

    fun stockHoldings(userId: String): List<StockHoldingEntity> = stockHoldingsRepository.getByUserId(userId)
}
