package com.valyalkin.portfolio.stocks.dividends

import com.valyalkin.portfolio.configuration.amqp.RabbitMQConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class DividendsService(
    private val rabbitTemplate: RabbitTemplate,
) {
    fun sendMessage(message: String) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY,
            message,
        )
    }

    fun triggerUpdate(ticker: String) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY,
            ticker,
        )
    }
}
