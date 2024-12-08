package com.valyalkin.portfolio.stocks.transactions

import com.valyalkin.portfolio.configuration.exception.BusinessException
import com.valyalkin.portfolio.stocks.holdings.HistoricalStockHoldingEntity
import com.valyalkin.portfolio.stocks.holdings.HistoricalStockHoldingsRepository
import com.valyalkin.portfolio.stocks.holdings.StockHoldingEntity
import com.valyalkin.portfolio.stocks.holdings.StockHoldingsRepository
import com.valyalkin.portfolio.stocks.pl.ReleasedProfitLossEntity
import com.valyalkin.portfolio.stocks.pl.ReleasedProfitLossEntityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class StockTransactionsService(
    private val stockTransactionsRepository: StockTransactionsRepository,
    private val stockHoldingsRepository: StockHoldingsRepository,
    private val releasedProfitLossEntityRepository: ReleasedProfitLossEntityRepository,
    private val historicalStockHoldingsRepository: HistoricalStockHoldingsRepository,
) {
    @Transactional
    fun addTransaction(stockTransactionDTO: StockTransactionDTO): StockTransactionEntity {
        val (userId, ticker, date, quantity, price, currency, transactionType) = stockTransactionDTO

        // TODO: Validate if ticker exists and currency is correct for this ticker
        // Get all transactions by user id and ticker, sorted by date
        val previousTransactions =
            stockTransactionsRepository.findByUserIdAndTickerAndCurrencyOrderByDateAsc(
                userId,
                ticker,
                currency,
            )

        // Compute latest stock holding and released P/L
        val transactions =
            previousTransactions
                .map {
                    StockTransaction(
                        date = it.date,
                        quantity = it.quantity,
                        price = it.price,
                        transactionType = it.transactionType,
                    )
                }.plus(
                    StockTransaction(
                        date = date,
                        quantity = quantity,
                        price = price,
                        transactionType = transactionType,
                    ),
                )

        processTransactions(transactions, userId, ticker, currency)

        return stockTransactionsRepository.save(
            StockTransactionEntity(
                userId = userId,
                ticker = ticker,
                date = date,
                quantity = quantity,
                price = price,
                transactionType = transactionType,
                currency = currency,
            ),
        )
    }

    private fun processTransactions(
        transactions: List<StockTransaction>,
        userId: String,
        ticker: String,
        currency: Currency,
    ) {
        transactions.sortedBy {
            it.date
        }
        if (transactions.first().transactionType == TransactionType.SELL) {
            throw BusinessException("Transaction cannot be processed, first transaction should be BUY")
        }

        // Released PL is affected by SELL transactions
        if (TransactionType.SELL in transactions.map { it.transactionType }) {
            releasedProfitLossEntityRepository.deleteByUserIdAndTickerAndCurrency(
                userId,
                ticker,
                currency,
            )
        }

        // Clear stock holdings
        stockHoldingsRepository.deleteByUserIdAndTicker(userId, ticker)

        // Clear historical transactions
        historicalStockHoldingsRepository.deleteByUserIdAndTicker(userId, ticker)
        val historicalHoldings = mutableListOf<HistoricalHolding>()

        var qty = transactions.first().quantity
        var averagePrice = transactions.first().price

        historicalHoldings.add(
            HistoricalHolding(
                quantity = qty,
                averagePrice = averagePrice,
                startDate = transactions.first().date,
                endDate = null,
            ),
        )

        for (i in 1..<transactions.size) {
            val currentTransactionType = transactions[i].transactionType

            val incomingPrice = transactions[i].price
            val incomingQuantity = transactions[i].quantity
            val incomingDate = transactions[i].date

            when (currentTransactionType) {
                TransactionType.BUY -> {
                    val previousTotal = averagePrice.multiply(BigDecimal.valueOf(qty))
                    val incomingTotal = incomingPrice.multiply(BigDecimal.valueOf(incomingQuantity))

                    val newTotal = previousTotal.plus(incomingTotal)
                    val newQuantity = qty.plus(incomingQuantity)
                    qty = newQuantity
                    averagePrice = newTotal.divide(BigDecimal.valueOf(qty), RoundingMode.DOWN)

                    // Historical holdings
                    if (historicalHoldings[i - 1].startDate == incomingDate) {
                        // Handle same date purchase
                        historicalHoldings[i - 1] =
                            historicalHoldings[i - 1].copy(
                                quantity = newQuantity,
                                averagePrice = averagePrice,
                            )
                    } else {
                        if (historicalHoldings[i - 1].endDate == null) {
                            historicalHoldings[i - 1] =
                                historicalHoldings[i - 1].copy(
                                    endDate = incomingDate.minusDays(1),
                                )
                        }
                        historicalHoldings.add(
                            HistoricalHolding(
                                quantity = qty,
                                averagePrice = averagePrice,
                                startDate = incomingDate,
                                endDate = null,
                            ),
                        )
                    }
                }

                TransactionType.SELL -> {
                    val newQuantity = qty.minus(incomingQuantity)
                    if (newQuantity < 0) {
                        throw BusinessException(
                            "Cannot add SELL transaction, cannot sell more than current holding at this time",
                        )
                    }
                    qty = newQuantity
                    val releasedPL = (incomingPrice.minus(averagePrice)).multiply(BigDecimal.valueOf(incomingQuantity))
                    releasedProfitLossEntityRepository.save(
                        ReleasedProfitLossEntity(
                            userId = userId,
                            ticker = ticker,
                            date = incomingDate,
                            amount = releasedPL,
                            currency = currency,
                        ),
                    )

                    // Historical holdings
                    if (historicalHoldings[i - 1].startDate == incomingDate) {
                        historicalHoldings[i - 1] =
                            historicalHoldings[i - 1].copy(
                                endDate = incomingDate,
                            )
                    } else {
                        historicalHoldings[i - 1] =
                            historicalHoldings[i - 1].copy(
                                endDate = incomingDate.minusDays(1),
                            )
                    }

                    // If new quantity is 0, then there is no new historical holding
                    if (newQuantity != 0L) {
                        historicalHoldings.add(
                            HistoricalHolding(
                                quantity = newQuantity,
                                averagePrice = averagePrice,
                                startDate = incomingDate,
                                endDate = null,
                            ),
                        )
                    }
                }
            }
        }

        if (qty != 0L) {
            stockHoldingsRepository.save(
                StockHoldingEntity(
                    userId = userId,
                    ticker = ticker,
                    quantity = qty,
                    averagePrice = averagePrice,
                    currency = currency,
                ),
            )
        }

        historicalHoldings.forEach {
            historicalStockHoldingsRepository.save(
                HistoricalStockHoldingEntity(
                    userId = userId,
                    ticker = ticker,
                    quantity = it.quantity,
                    averagePrice = it.averagePrice,
                    currency = currency,
                    startDate = it.startDate,
                    endDate = it.endDate,
                ),
            )
        }
    }

    @Transactional
    fun deleteTransaction(transactionId: String) {
        val id =
            try {
                UUID.fromString(transactionId)
            } catch (e: IllegalArgumentException) {
                throw BusinessException("Transaction id is in incorrect format")
            }

        val transactionToDelete =
            stockTransactionsRepository.findById(id).getOrNull()
                ?: throw BusinessException("Transaction with id $id doesn't exist")
        // Delete transaction from transactions repository
        stockTransactionsRepository.deleteById(id)

        val userId = transactionToDelete.userId
        val ticker = transactionToDelete.ticker
        val currency = transactionToDelete.currency
        val transactions =
            stockTransactionsRepository
                .findByUserIdAndTickerAndCurrencyOrderByDateAsc(
                    userId,
                    ticker,
                    currency,
                ).map {
                    StockTransaction(
                        date = it.date,
                        quantity = it.quantity,
                        price = it.price,
                        transactionType = it.transactionType,
                    )
                }

        releasedProfitLossEntityRepository.deleteByUserIdAndTickerAndCurrency(
            userId,
            ticker,
            currency,
        )

        processTransactions(transactions, userId, ticker, currency)
    }

    fun getTransactions(
        userId: String,
        currency: Currency,
        page: Int,
        pageSize: Int?,
    ): Page<StockTransactionEntity> {
        val sortBy = Sort.by(DATE_FIELD).descending()
        val pageable = PageRequest.of(page - 1, pageSize ?: DEFAULT_PAGE_SIZE, sortBy)
        return stockTransactionsRepository.findByUserIdAndCurrency(
            userId,
            currency,
            pageable,
        )
    }

    private data class StockTransaction(
        val date: LocalDate,
        val quantity: Long,
        val price: BigDecimal,
        val transactionType: TransactionType,
    )

    private data class HistoricalHolding(
        val quantity: Long,
        val averagePrice: BigDecimal,
        val startDate: LocalDate,
        val endDate: LocalDate?,
    )

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val DATE_FIELD = "date"
    }
}
