package com.valyalkin.portfolio.stocks.holdings

import org.springframework.stereotype.Service

@Service
class HistoricalStockHoldingsService(
    private val historicalStockHoldingsRepository: HistoricalStockHoldingsRepository,
) {
    fun historicalHoldings(userId: String) = historicalStockHoldingsRepository.findByUserId(userId)
}
