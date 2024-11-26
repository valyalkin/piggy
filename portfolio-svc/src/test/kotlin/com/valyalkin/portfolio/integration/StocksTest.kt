package com.valyalkin.portfolio.integration

import com.valyalkin.portfolio.configuration.Mapper
import com.valyalkin.portfolio.data.MarketDataService
import com.valyalkin.portfolio.data.TickerData
import com.valyalkin.portfolio.stocks.holdings.HistoricalStockHoldingEntity
import com.valyalkin.portfolio.stocks.holdings.HistoricalStockHoldingsRepository
import com.valyalkin.portfolio.stocks.holdings.StockHoldingEntity
import com.valyalkin.portfolio.stocks.holdings.StockHoldingsRepository
import com.valyalkin.portfolio.stocks.pl.ReleasedProfitLossEntity
import com.valyalkin.portfolio.stocks.pl.ReleasedProfitLossEntityRepository
import com.valyalkin.portfolio.stocks.transactions.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class StocksTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var stockTransactionsRepository: StockTransactionsRepository

    @Autowired
    private lateinit var stockHoldingsRepository: StockHoldingsRepository

    @Autowired
    private lateinit var releasedProfitLossEntityRepository: ReleasedProfitLossEntityRepository

    @Autowired
    private lateinit var historicalStockHoldingsRepository: HistoricalStockHoldingsRepository

    @MockBean
    private lateinit var marketDataService: MarketDataService

    @BeforeEach
    fun cleanUp() {
        stockTransactionsRepository.deleteAll()
        stockHoldingsRepository.deleteAll()
        releasedProfitLossEntityRepository.deleteAll()
        historicalStockHoldingsRepository.deleteAll()

        Mockito.`when`(marketDataService.getTickerData("APPL")).thenReturn(
            TickerData("APPL", Currency.USD),
        )
    }

    private val testUserId = "test"
    private val testDate =
        LocalDate.of(2023, 10, 10)

    private val testCurrency = Currency.USD
    private val testTicker = "APPL"

    private val testQuantity = 10L
    private val testPrice = BigDecimal.valueOf(100.5)

    private fun testStockTransactionDto(transactionType: TransactionType) =
        StockTransactionDTO(
            userId = testUserId,
            ticker = testTicker,
            date = testDate,
            quantity = testQuantity,
            price = testPrice,
            currency = testCurrency,
            transactionType = transactionType,
        )

    private data class TestData(
        val transactionType: TransactionType,
        val quantity: Long,
        val price: BigDecimal,
        val minusDays: Long,
    )

    @Nested
    inner class AddTransactionTests {
        @Test
        fun `Buy - Should add new transaction and add new stock holding`() {
            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.BUY,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().isCreated,
                    ).andReturn()
                    .response.contentAsString

            val transaction = Mapper.objectMapper.readValue(transactionResponse, StockTransactionEntity::class.java)
            val id = transaction.id

            // Check transaction
            val savedTransaction = stockTransactionsRepository.findById(id)
            assertThat(savedTransaction.isPresent).isTrue()
            savedTransaction.get().let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.price).isEqualByComparingTo(testPrice)
                assertThat(it.transactionType).isEqualTo(TransactionType.BUY)
                assertThat(it.currency).isEqualTo(testCurrency)
            }

            // Historical holdings
            val historicalHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    userId = testUserId,
                    ticker = testTicker,
                )

            assertThat(historicalHoldings).isNotEmpty
            assertThat(historicalHoldings.size).isEqualTo(1)
            historicalHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.averagePrice).isEqualByComparingTo(testPrice)
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isNull()
            }

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(1)
            stockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.averagePrice).isEqualByComparingTo(testPrice)
            }
        }

        @Test
        fun `Sell - Should fail if first transaction is SELL`() {
            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.SELL,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().is4xxClientError,
                    ).andReturn()
                    .response.contentAsString

            assertThat(transactionResponse).contains("Transaction cannot be processed, first transaction should be BUY")
        }

        @Test
        fun `Buy - Should add new transaction and update the existing stock holding with new average price and quantity`() {
            val previousQuantity = 5L
            val previousAveragePrice = BigDecimal.valueOf(100L)
            val currency = Currency.USD

            // Previous transaction and stock holding
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.minusDays(1), // one day before
                    quantity = previousQuantity,
                    price = previousAveragePrice,
                    currency = testCurrency,
                    transactionType = TransactionType.BUY,
                ),
            )

            stockHoldingsRepository.save(
                StockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = previousQuantity,
                    averagePrice = previousAveragePrice,
                    currency = currency,
                ),
            )

            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.BUY,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().isCreated,
                    ).andReturn()
                    .response.contentAsString

            val transaction = Mapper.objectMapper.readValue(transactionResponse, StockTransactionEntity::class.java)
            val id = transaction.id

            // Check transaction
            val savedTransaction = stockTransactionsRepository.findById(id)
            assertThat(savedTransaction.isPresent).isTrue()
            savedTransaction.get().let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.price).isEqualByComparingTo(testPrice)
                assertThat(it.transactionType).isEqualTo(TransactionType.BUY)
                assertThat(it.currency).isEqualTo(testCurrency)
            }

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(1)
            stockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity.plus(previousQuantity))
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100.33))
            }

            // Historical holdings
            val historicalHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    userId = testUserId,
                    ticker = testTicker,
                )

            assertThat(historicalHoldings).isNotEmpty
            assertThat(historicalHoldings.size).isEqualTo(2)

            historicalHoldings.sortedBy {
                it.startDate
            }
            historicalHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(previousQuantity)
                assertThat(it.averagePrice).isEqualByComparingTo(previousAveragePrice)
                assertThat(it.startDate).isEqualTo(testDate.minusDays(1))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(1))
            }
            historicalHoldings[1].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity.plus(previousQuantity))
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100.33))
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isNull()
            }
        }

        @Test
        fun `Buy - Should handle historical holdings for the same day`() {
            val previousQuantity = 5L
            val previousAveragePrice = BigDecimal.valueOf(100L)
            val currency = Currency.USD

            // Previous transaction and stock holding
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate, // same day
                    quantity = previousQuantity,
                    price = previousAveragePrice,
                    currency = testCurrency,
                    transactionType = TransactionType.BUY,
                ),
            )

            stockHoldingsRepository.save(
                StockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = previousQuantity,
                    averagePrice = previousAveragePrice,
                    currency = currency,
                ),
            )

            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.BUY,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().isCreated,
                    ).andReturn()
                    .response.contentAsString

            val transaction = Mapper.objectMapper.readValue(transactionResponse, StockTransactionEntity::class.java)
            val id = transaction.id

            // Check transaction
            val savedTransaction = stockTransactionsRepository.findById(id)
            assertThat(savedTransaction.isPresent).isTrue()
            savedTransaction.get().let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.price).isEqualByComparingTo(testPrice)
                assertThat(it.transactionType).isEqualTo(TransactionType.BUY)
                assertThat(it.currency).isEqualTo(testCurrency)
            }

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(1)
            stockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity.plus(previousQuantity))
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100.33))
            }

            // Historical holdings
            val historicalHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    userId = testUserId,
                    ticker = testTicker,
                )

            assertThat(historicalHoldings).isNotEmpty
            assertThat(historicalHoldings.size).isEqualTo(1)

            historicalHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity.plus(previousQuantity))
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100.33))
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isNull()
            }
        }

        @Test
        fun `Sell - Should add new transaction and update the existing stock holding with new quantity, record released pl`() {
            val previousQuantity = testQuantity
            val previousAveragePrice = BigDecimal.valueOf(80L)

            // Previous transaction and stock holding

            recordPreviousTransaction(
                quantity = previousQuantity,
                price = previousAveragePrice,
                minusDays = 20L,
                transactionType = TransactionType.BUY,
            )

            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.SELL,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().isCreated,
                    ).andReturn()
                    .response.contentAsString

            val transaction = Mapper.objectMapper.readValue(transactionResponse, StockTransactionEntity::class.java)
            val id = transaction.id

            // Check transaction
            val savedTransaction = stockTransactionsRepository.findById(id)
            assertThat(savedTransaction.isPresent).isTrue()
            savedTransaction.get().let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.price).isEqualByComparingTo(testPrice)
                assertThat(it.transactionType).isEqualTo(TransactionType.SELL)
                assertThat(it.currency).isEqualTo(testCurrency)
            }

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(0)

            // Check released profit
            val releasedPL =
                releasedProfitLossEntityRepository.getByUserIdAndTickerAndCurrency(
                    testUserId,
                    testTicker,
                    testCurrency,
                )
            assertThat(releasedPL.size).isEqualTo(1)
            releasedPL[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.amount).isEqualByComparingTo(BigDecimal.valueOf(205.0))
            }

            // Check historical stock holdings

            val historicalStockHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    testUserId,
                    testTicker,
                )

            assertThat(historicalStockHoldings.size).isEqualTo(1)

            historicalStockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.averagePrice).isEqualByComparingTo(previousAveragePrice)
                assertThat(it.startDate).isEqualTo(testDate.minusDays(20))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(1))
            }
        }

        @Test
        fun `Sell - Should add new transaction and update the existing stock holding with new quantity for the same day, record released pl`() {
            val previousQuantity = testQuantity
            val previousAveragePrice = BigDecimal.valueOf(80L)

            // Previous transaction and stock holding

            recordPreviousTransaction(
                quantity = previousQuantity,
                price = previousAveragePrice,
                minusDays = 0,
                transactionType = TransactionType.BUY,
            )

            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.SELL,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().isCreated,
                    ).andReturn()
                    .response.contentAsString

            val transaction = Mapper.objectMapper.readValue(transactionResponse, StockTransactionEntity::class.java)
            val id = transaction.id

            // Check transaction
            val savedTransaction = stockTransactionsRepository.findById(id)
            assertThat(savedTransaction.isPresent).isTrue()
            savedTransaction.get().let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.price).isEqualByComparingTo(testPrice)
                assertThat(it.transactionType).isEqualTo(TransactionType.SELL)
                assertThat(it.currency).isEqualTo(testCurrency)
            }

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(0)

            // Check released profit
            val releasedPL =
                releasedProfitLossEntityRepository.getByUserIdAndTickerAndCurrency(
                    testUserId,
                    testTicker,
                    testCurrency,
                )
            assertThat(releasedPL.size).isEqualTo(1)
            releasedPL[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.date).isEqualTo(testDate)
                assertThat(it.amount).isEqualByComparingTo(BigDecimal.valueOf(205.0))
            }

            // Check historical stock holdings

            val historicalStockHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    testUserId,
                    testTicker,
                )

            assertThat(historicalStockHoldings.size).isEqualTo(1)

            historicalStockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(testQuantity)
                assertThat(it.averagePrice).isEqualByComparingTo(previousAveragePrice)
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isEqualTo(testDate)
            }
        }

        @Test
        fun `BUY and SELL - Should add new transaction and update the existing stock holding with new quantity, record released pl`() {
            val transactions =
                listOf(
                    TestData(TransactionType.BUY, 10, BigDecimal.valueOf(100L), 10),
                    TestData(TransactionType.BUY, 5, BigDecimal.valueOf(90L), 9),
                    TestData(TransactionType.BUY, 10, BigDecimal.valueOf(110L), 8),
                    TestData(TransactionType.SELL, 5, BigDecimal.valueOf(120L), 7),
                    TestData(TransactionType.BUY, 15, BigDecimal.valueOf(100L), 6),
                    TestData(TransactionType.SELL, 5, BigDecimal.valueOf(80L), 5),
                )

            transactions.forEach {
                recordPreviousTransaction(
                    quantity = it.quantity,
                    price = it.price,
                    minusDays = it.minusDays,
                    transactionType = it.transactionType,
                )
            }

            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.BUY,
                ).copy(
                    quantity = 2L,
                    price = BigDecimal.valueOf(70L),
                    date = testDate,
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().isCreated,
                    ).andReturn()
                    .response.contentAsString

            val transaction = Mapper.objectMapper.readValue(transactionResponse, StockTransactionEntity::class.java)

            // Check transaction
            val savedTransactions = stockTransactionsRepository.findAll()
            assertThat(savedTransactions.count()).isEqualTo(7)

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(1)
            stockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(32L)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(99.18))
            }

            // Check released profit
            val releasedPL =
                releasedProfitLossEntityRepository
                    .getByUserIdAndTickerAndCurrency(
                        testUserId,
                        testTicker,
                        testCurrency,
                    ).sortedBy {
                        it.date
                    }

            assertThat(releasedPL.size).isEqualTo(2)
            assertThat(releasedPL[0].amount).isEqualByComparingTo(BigDecimal.valueOf(90.05))
            assertThat(releasedPL[1].amount).isEqualByComparingTo(BigDecimal.valueOf(-105.65))

            // Check historical holdings

            val historicalHoldings = historicalStockHoldingsRepository.findByUserIdAndTicker(testUserId, testTicker)

            assertThat(historicalHoldings.size).isEqualTo(7)

            historicalHoldings[0].let {
                assertThat(it.quantity).isEqualTo(10)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100))
                assertThat(it.startDate).isEqualTo(testDate.minusDays(10))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(10))
            }
            historicalHoldings[1].let {
                assertThat(it.quantity).isEqualTo(15)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(96.66))
                assertThat(it.startDate).isEqualTo(testDate.minusDays(9))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(9))
            }
            historicalHoldings[2].let {
                assertThat(it.quantity).isEqualTo(25)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(101.99))
                assertThat(it.startDate).isEqualTo(testDate.minusDays(8))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(8))
            }
            historicalHoldings[3].let {
                assertThat(it.quantity).isEqualTo(20)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(101.99))
                assertThat(it.startDate).isEqualTo(testDate.minusDays(7))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(7))
            }
            historicalHoldings[4].let {
                assertThat(it.quantity).isEqualTo(35)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(101.13))
                assertThat(it.startDate).isEqualTo(testDate.minusDays(6))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(6))
            }
            historicalHoldings[5].let {
                assertThat(it.quantity).isEqualTo(30)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(101.13))
                assertThat(it.startDate).isEqualTo(testDate.minusDays(5))
                assertThat(it.endDate).isEqualTo(testDate.minusDays(1))
            }
            historicalHoldings[6].let {
                assertThat(it.quantity).isEqualTo(32)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(99.18))
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isNull()
            }
        }

        @Test
        fun `Sell - Should fail if quantity to sell is bigger than holdings`() {
            val previousQuantity = testQuantity
            val previousAveragePrice = BigDecimal.valueOf(80L)
            val currency = Currency.USD

            // Previous transaction and stock holding
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.minusDays(1), // one day before
                    quantity = previousQuantity,
                    price = previousAveragePrice,
                    currency = testCurrency,
                    transactionType = TransactionType.BUY,
                ),
            )

            stockHoldingsRepository.save(
                StockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = previousQuantity,
                    averagePrice = previousAveragePrice,
                    currency = currency,
                ),
            )

            val stockTransactionDTO =
                testStockTransactionDto(
                    transactionType = TransactionType.SELL,
                ).copy(
                    quantity = testQuantity.plus(20), // Quantity is bigger
                )

            val transactionResponse =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/v1/stocks/transaction")
                            .contentType("application/json")
                            .content(
                                Mapper.objectMapper.writeValueAsString(stockTransactionDTO),
                            ),
                    ).andExpect(
                        status().is4xxClientError,
                    ).andReturn()
                    .response.contentAsString

            assertThat(transactionResponse).contains("Cannot add SELL transaction, cannot sell more than current holding at this time")
        }
    }

    @Nested
    inner class DeleteTransactionTests {
        @Test
        fun `Delete - Should delete SELL transaction and update new stock holding, PL and historical transactions`() {
            // Save BUY AAPL, qty 10, price 100
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate,
                    quantity = 10L,
                    price = BigDecimal.valueOf(100),
                    currency = testCurrency,
                    transactionType = TransactionType.BUY,
                ),
            )

            // Save SELL AAPL, qty 5, price 120
            val transactionToDelete =
                stockTransactionsRepository.save(
                    StockTransactionEntity(
                        userId = testUserId,
                        ticker = testTicker,
                        date = testDate.plusMonths(1L),
                        quantity = 5L,
                        price = BigDecimal.valueOf(120),
                        currency = testCurrency,
                        transactionType = TransactionType.SELL,
                    ),
                )

            // PL corresponding to the current state
            releasedProfitLossEntityRepository.save(
                ReleasedProfitLossEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(1L),
                    amount = BigDecimal.valueOf(100),
                    currency = testCurrency,
                ),
            )

            // Stock holding corresponding to the current state
            stockHoldingsRepository.save(
                StockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = 5L,
                    averagePrice = BigDecimal.valueOf(100),
                    currency = testCurrency,
                ),
            )

            // Historical holdings corresponding to the current state
            historicalStockHoldingsRepository.save(
                HistoricalStockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = 10L,
                    averagePrice = BigDecimal.valueOf(100L),
                    startDate = testDate,
                    endDate = testDate.plusMonths(1L).minusDays(1L),
                    currency = testCurrency,
                ),
            )

            historicalStockHoldingsRepository.save(
                HistoricalStockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = 5L,
                    averagePrice = BigDecimal.valueOf(100L),
                    startDate = testDate.plusMonths(1L),
                    endDate = null,
                    currency = testCurrency,
                ),
            )

            val transactionIdToDelete = transactionToDelete.id.toString()

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .delete("/v1/stocks/transaction/$transactionIdToDelete"),
                ).andExpect(
                    status().is2xxSuccessful,
                )

            // Check that transaction is deleted

            assertThat(stockTransactionsRepository.findById(transactionToDelete.id)).isEmpty

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(1)
            stockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(10L)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100L))
            }

            // Check released profit
            val releasedPL =
                releasedProfitLossEntityRepository
                    .getByUserIdAndTickerAndCurrency(
                        testUserId,
                        testTicker,
                        testCurrency,
                    )

            val historicalStockHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    userId = testUserId,
                    ticker = testTicker,
                )

            assertThat(historicalStockHoldings.size).isEqualTo(1)
            historicalStockHoldings[0].let {
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isNull()
                assertThat(it.quantity).isEqualTo(10L)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100))
            }

            assertThat(releasedPL).isEmpty()
        }

        @Test
        fun `Delete - Should fail to delete transaction with malformed id`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .delete("/v1/stocks/transaction/123"),
                ).andExpect(
                    status().is4xxClientError,
                )
        }

        @Test
        fun `Delete - Should not delete transaction if it leaves SELL to be the first one`() {
            // Save BUY AAPL, qty 10, price 100
            val transactionToDelete =
                stockTransactionsRepository.save(
                    StockTransactionEntity(
                        userId = testUserId,
                        ticker = testTicker,
                        date = testDate,
                        quantity = 10L,
                        price = BigDecimal.valueOf(100),
                        currency = testCurrency,
                        transactionType = TransactionType.BUY,
                    ),
                )

            // Save SELL AAPL, qty 5, price 120

            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(1L),
                    quantity = 5L,
                    price = BigDecimal.valueOf(120),
                    currency = testCurrency,
                    transactionType = TransactionType.SELL,
                ),
            )

            // PL corresponding to the current state
            releasedProfitLossEntityRepository.save(
                ReleasedProfitLossEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(1L),
                    amount = BigDecimal.valueOf(100),
                    currency = testCurrency,
                ),
            )

            // Stock holding corresponding to the current state
            stockHoldingsRepository.save(
                StockHoldingEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    quantity = 5L,
                    averagePrice = BigDecimal.valueOf(100),
                    currency = testCurrency,
                ),
            )

            val transactionIdToDelete = transactionToDelete.id.toString()

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .delete("/v1/stocks/transaction/$transactionIdToDelete"),
                ).andExpect(
                    status().is4xxClientError,
                )

            assertThat(
                stockTransactionsRepository
                    .findByUserIdAndTickerAndCurrencyOrderByDateAsc(
                        testUserId,
                        testTicker,
                        testCurrency,
                    ).size,
            ).isEqualTo(2)

            assertThat(stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker).size).isEqualTo(1)
            assertThat(
                releasedProfitLossEntityRepository
                    .getByUserIdAndTickerAndCurrency(
                        testUserId,
                        testTicker,
                        testCurrency,
                    ).size,
            ).isEqualTo(1)
        }

        @Test
        fun `Delete - Should delete SELL transaction and create new holding and pl`() {
            // Save BUY AAPL, qty 10, price 100
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate,
                    quantity = 10L,
                    price = BigDecimal.valueOf(100),
                    currency = testCurrency,
                    transactionType = TransactionType.BUY,
                ),
            )

            // Save SELL AAPL, qty 5, price 120
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(1L),
                    quantity = 5L,
                    price = BigDecimal.valueOf(120),
                    currency = testCurrency,
                    transactionType = TransactionType.SELL,
                ),
            )

            // Save SELL AAPL, qty 5, price 120
            val transactionToDelete =
                stockTransactionsRepository.save(
                    StockTransactionEntity(
                        userId = testUserId,
                        ticker = testTicker,
                        date = testDate.plusMonths(2L),
                        quantity = 5L,
                        price = BigDecimal.valueOf(140),
                        currency = testCurrency,
                        transactionType = TransactionType.SELL,
                    ),
                )

            // PL corresponding to the current state
            releasedProfitLossEntityRepository.save(
                ReleasedProfitLossEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(1L),
                    amount = BigDecimal.valueOf(100),
                    currency = testCurrency,
                ),
            )

            releasedProfitLossEntityRepository.save(
                ReleasedProfitLossEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(2L),
                    amount = BigDecimal.valueOf(200),
                    currency = testCurrency,
                ),
            )

            // No stock holdings in this case because it is all sold out

            val transactionIdToDelete = transactionToDelete.id.toString()

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .delete("/v1/stocks/transaction/$transactionIdToDelete"),
                ).andExpect(
                    status().is2xxSuccessful,
                )

            // Check that transaction is deleted
            assertThat(stockTransactionsRepository.findById(transactionToDelete.id)).isEmpty

            // Check stock holding
            val stockHoldings = stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker)
            assertThat(stockHoldings.size).isEqualTo(1)
            stockHoldings[0].let {
                assertThat(it.userId).isEqualTo(testUserId)
                assertThat(it.ticker).isEqualTo(testTicker)
                assertThat(it.quantity).isEqualTo(5L)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100L))
            }

            // Check released profit
            val releasedPL =
                releasedProfitLossEntityRepository
                    .getByUserIdAndTickerAndCurrency(
                        testUserId,
                        testTicker,
                        testCurrency,
                    )

            assertThat(releasedPL.size).isEqualTo(1)
            assertThat(releasedPL[0].amount).isEqualByComparingTo(BigDecimal.valueOf(100L))
            assertThat(releasedPL[0].date).isEqualTo(testDate.plusMonths(1L))

            // Check historical stock holdings
            val historicalStockHoldings =
                historicalStockHoldingsRepository.findByUserIdAndTicker(
                    testUserId,
                    testTicker,
                )

            assertThat(historicalStockHoldings.size).isEqualTo(2)

            historicalStockHoldings[0].let {
                assertThat(it.startDate).isEqualTo(testDate)
                assertThat(it.endDate).isEqualTo(testDate.plusMonths(1).minusDays(1))
                assertThat(it.quantity).isEqualTo(10L)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100))
            }

            historicalStockHoldings[1].let {
                assertThat(it.startDate).isEqualTo(testDate.plusMonths(1))
                assertThat(it.endDate).isNull()
                assertThat(it.quantity).isEqualTo(5L)
                assertThat(it.averagePrice).isEqualByComparingTo(BigDecimal.valueOf(100))
            }
        }

        @Test
        fun `Delete - Should not delete transaction if SELL quantity is bigger than holding`() {
            // Save BUY AAPL, qty 10, price 100
            val transactionToDelete =
                stockTransactionsRepository.save(
                    StockTransactionEntity(
                        userId = testUserId,
                        ticker = testTicker,
                        date = testDate,
                        quantity = 10L,
                        price = BigDecimal.valueOf(100),
                        currency = testCurrency,
                        transactionType = TransactionType.BUY,
                    ),
                )

            // Save BUY AAPL, qty 5, price 80
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(2L),
                    quantity = 5L,
                    price = BigDecimal.valueOf(80),
                    currency = testCurrency,
                    transactionType = TransactionType.BUY,
                ),
            )

            // Save SELL AAPL, qty 5, price 120
            stockTransactionsRepository.save(
                StockTransactionEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(2L),
                    quantity = 15L,
                    price = BigDecimal.valueOf(200),
                    currency = testCurrency,
                    transactionType = TransactionType.SELL,
                ),
            )

            // PL corresponding to the current state
            releasedProfitLossEntityRepository.save(
                ReleasedProfitLossEntity(
                    userId = testUserId,
                    ticker = testTicker,
                    date = testDate.plusMonths(2L),
                    amount = BigDecimal.valueOf(1600.05),
                    currency = testCurrency,
                ),
            )

            // There is no current holding for current state

            val transactionIdToDelete = transactionToDelete.id.toString()

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .delete("/v1/stocks/transaction/$transactionIdToDelete"),
                ).andExpect(
                    status().is4xxClientError,
                )

            assertThat(
                stockTransactionsRepository
                    .findByUserIdAndTickerAndCurrencyOrderByDateAsc(
                        testUserId,
                        testTicker,
                        testCurrency,
                    ).size,
            ).isEqualTo(3)

            assertThat(stockHoldingsRepository.getByUserIdAndTicker(testUserId, testTicker).size).isEqualTo(0)
            assertThat(
                releasedProfitLossEntityRepository
                    .getByUserIdAndTickerAndCurrency(
                        testUserId,
                        testTicker,
                        testCurrency,
                    ).size,
            ).isEqualTo(1)
        }
    }

    private fun recordPreviousTransaction(
        quantity: Long,
        price: BigDecimal,
        minusDays: Long,
        transactionType: TransactionType,
    ) {
        // Previous transaction and stock holding
        stockTransactionsRepository.save(
            StockTransactionEntity(
                userId = testUserId,
                ticker = testTicker,
                date = testDate.minusDays(minusDays), // one day before
                quantity = quantity,
                price = price,
                currency = testCurrency,
                transactionType = transactionType,
            ),
        )
    }
}
