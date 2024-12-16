package com.valyalkin.market.dividends;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dividends")
@NoArgsConstructor
@ToString
@Getter
@Setter
public class DividendsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "ticker", nullable = false)
    private String ticker;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal dividend;
}
