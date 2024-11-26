package com.valyalkin.portfolio.data.tickers

import com.valyalkin.portfolio.stocks.transactions.Country
import com.valyalkin.portfolio.stocks.transactions.Currency
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

@Entity
@Table(name = "tickers")
class TickersEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) val id: UUID = UUID.randomUUID(),
    @Column(name = "symbol", unique = true) val symbol: String = "",
    @Enumerated(EnumType.STRING) val currency: Currency = Currency.USD,
    val name: String = "",
    @Column(name = "has_eod") val hasEodPrice: Boolean = false,
    val exchange: String = "",
    val acronym: String = "",
    val mic: String = "",
    val country: String = "",
    @Enumerated(EnumType.STRING) @Column(name = "country_code") val countryCode: Country = Country.US,
)

interface TickersRepository : JpaRepository<TickersEntity, UUID> {
    fun findBySymbol(symbol: String): TickersEntity?
}
