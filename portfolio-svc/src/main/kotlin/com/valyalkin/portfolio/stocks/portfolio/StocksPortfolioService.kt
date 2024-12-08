package com.valyalkin.portfolio.stocks.portfolio

import com.valyalkin.portfolio.market.MarketDataGateway
import com.valyalkin.portfolio.stocks.holdings.StockHoldingsService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class StocksPortfolioService(
    private val stockHoldingsService: StockHoldingsService,
    private val marketDataGateway: MarketDataGateway,
) {
    fun getPortfolio(userId: String): StocksPortfolio {
        val holdings = stockHoldingsService.stockHoldings(userId = userId)

        val overview =
            holdings.map { holding ->
                val lastPrice = marketDataGateway.latestPrice(holding.ticker)
                val value = holding.averagePrice.multiply(BigDecimal.valueOf(holding.quantity))
                val marketValue = lastPrice.price.multiply(BigDecimal.valueOf(holding.quantity))

                val totalGain = marketValue.minus(value)
                val totalGainPercentage = totalGain.multiply(BigDecimal.valueOf(100L)).divide(value, RoundingMode.HALF_UP)
                StockOverview(
                    holding.ticker,
                    quantity = holding.quantity,
                    averagePrice = holding.averagePrice,
                    lastPrice = lastPrice.price,
                    totalGain = totalGain,
                    totalGainPercentage = totalGainPercentage,
                    asOf = lastPrice.latestDate,
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
