package com.valyalkin.market.ticker;

import com.valyalkin.market.eod.Currency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;


@NoArgsConstructor
@Entity
@Table(name = "tickers")
@ToString
@Getter
@Setter
public class TickersEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(name = "symbol", unique = true)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private String name;

    @Column(name = "has_eod", nullable = false)
    private boolean hasEodPrice;

    @Column(nullable = false)
    private String exchange;

    @Column(nullable = false)
    private String acronym;

    @Column(nullable = false)
    private String mic;

    @Column(nullable = false)
    private String country;

    @Column(name = "country_code", nullable = false)
    private String countryCode;
}
