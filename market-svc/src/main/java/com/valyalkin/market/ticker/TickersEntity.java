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
    private Currency currency;

    private String name;

    @Column(name = "has_eod")
    private boolean hasEodPrice;

    private String exchange;

    private String acronym;

    private String mic;

    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "country_code")
    private Country countryCode;
}
