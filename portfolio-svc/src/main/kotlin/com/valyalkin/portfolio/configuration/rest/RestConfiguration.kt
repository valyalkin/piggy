package com.valyalkin.portfolio.configuration.rest

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestConfiguration {
    @Bean
    fun restClient() = RestClient.builder().build()
}
