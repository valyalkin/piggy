package com.valyalkin.portfolio.stocks

import com.valyalkin.portfolio.stocks.dividends.DividendsService
import com.valyalkin.portfolio.stocks.holdings.HistoricalStockHoldingEntity
import com.valyalkin.portfolio.stocks.holdings.HistoricalStockHoldingsService
import com.valyalkin.portfolio.stocks.holdings.StockHoldingEntity
import com.valyalkin.portfolio.stocks.holdings.StockHoldingsService
import com.valyalkin.portfolio.stocks.portfolio.StocksPortfolio
import com.valyalkin.portfolio.stocks.portfolio.StocksPortfolioService
import com.valyalkin.portfolio.stocks.transactions.Currency
import com.valyalkin.portfolio.stocks.transactions.StockTransactionDTO
import com.valyalkin.portfolio.stocks.transactions.StockTransactionEntity
import com.valyalkin.portfolio.stocks.transactions.StockTransactionsService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/stocks")
class StocksController(
    private val transactionsService: StockTransactionsService,
    private val holdingsService: StockHoldingsService,
    private val portfolioService: StocksPortfolioService,
    private val historicalStockHoldingsService: HistoricalStockHoldingsService,
    private val dividendsService: DividendsService,
) {
    @PostMapping("/transaction")
    @ResponseStatus(HttpStatus.CREATED)
    fun addTransaction(
        @RequestBody @Valid stockTransactionDTO: StockTransactionDTO,
    ): StockTransactionEntity =
        transactionsService.addTransaction(stockTransactionDTO).also {
            dividendsService.triggerUpdate(stockTransactionDTO.ticker)
        }

    @DeleteMapping("/transaction/{transactionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTransaction(
        @PathVariable transactionId: String,
    ) = transactionsService.deleteTransaction(transactionId)

    @GetMapping("/transactions")
    @ResponseStatus(HttpStatus.OK)
    fun transactions(
        @RequestParam(name = "userId") userId: String,
        @RequestParam(name = "currency") currency: Currency,
        @RequestParam(name = "page") page: Int,
        @RequestParam(name = "pageSize", required = false) pageSize: Int?,
    ): Page<StockTransactionEntity> = transactionsService.getTransactions(userId, currency, page, pageSize)

    @GetMapping("/holdings")
    @ResponseStatus(HttpStatus.OK)
    fun holdings(
        @RequestParam(name = "userId") userId: String,
        @RequestParam(name = "currency") currency: Currency,
    ): List<StockHoldingEntity> = holdingsService.stockHoldings(userId, currency)

    @GetMapping("/portfolio")
    @ResponseStatus(HttpStatus.OK)
    fun portfolio(
        @RequestParam(name = "userId") userId: String,
    ): StocksPortfolio = portfolioService.getPortfolio(userId)

    @GetMapping("/historicalHoldings")
    @ResponseStatus(HttpStatus.OK)
    fun historicalHoldings(
        @RequestParam(name = "userId") userId: String,
    ): List<HistoricalStockHoldingEntity> = historicalStockHoldingsService.historicalHoldings(userId)

    @GetMapping("/dividends")
    @ResponseStatus(HttpStatus.OK)
    fun dividends(
        @RequestParam(name = "userId") userId: String,
    ) = dividendsService.sendMessage("Hello")
}
