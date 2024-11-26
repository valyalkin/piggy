package com.valyalkin.portfolio.stocks.portfolio

import com.valyalkin.portfolio.data.MarketDataService
import com.valyalkin.portfolio.stocks.holdings.StockHoldingsService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class StocksPortfolioService(
    private val stockHoldingsService: StockHoldingsService,
    private val marketDataService: MarketDataService,
) {
    fun getPortfolio(userId: String): StocksPortfolio {
        val holdings = stockHoldingsService.stockHoldings(userId = userId)

        val overview =
            holdings.map { holding ->
                val lastPrice = marketDataService.getLatestEodPrice(holding.ticker)
                val value = holding.averagePrice.multiply(BigDecimal.valueOf(holding.quantity))
                val marketValue = lastPrice.first.multiply(BigDecimal.valueOf(holding.quantity))

                val totalGain = marketValue.minus(value)
                val totalGainPercentage = totalGain.multiply(BigDecimal.valueOf(100L)).divide(value, RoundingMode.HALF_UP)
                StockOverview(
                    holding.ticker,
                    quantity = holding.quantity,
                    averagePrice = holding.averagePrice,
                    lastPrice = lastPrice.first,
                    totalGain = totalGain,
                    totalGainPercentage = totalGainPercentage,
                    asOf = lastPrice.second,
                )
            }

        return StocksPortfolio(
            data = overview,
        )
    }
}

data class StocksPortfolio(
    val data: List<StockOverview>,
)

data class StockOverview(
    val ticker: String,
    val quantity: Long,
    val averagePrice: BigDecimal,
    val lastPrice: BigDecimal,
    val totalGain: BigDecimal,
    val totalGainPercentage: BigDecimal,
    val asOf: LocalDate,
)
