package com.valyalkin.market.ticker;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TickersRepository extends JpaRepository<TickersEntity, UUID> {
}
