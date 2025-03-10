package com.valyalkin.portfolio.stocks.transactions

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.LocalDate

data class StockTransactionDTO(
    @field:NotBlank val userId: String,
    @field:NotBlank val ticker: String,
    val date: LocalDate,
    @field:Min(value = 1, message = "Quantity must be greater than 0") val quantity: Long,
    @field:DecimalMin(value = "0.01", message = "Price must be greater than 0") val price: BigDecimal,
    val currency: Currency,
    val transactionType: TransactionType,
)
