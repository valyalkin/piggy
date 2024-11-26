package com.valyalkin.portfolio

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PortfolioSvcApplication

fun main(args: Array<String>) {
    runApplication<PortfolioSvcApplication>(*args)
}
