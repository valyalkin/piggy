package com.valyalkin.portfolio.configuration.amqp

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {
    companion object {
        const val QUEUE_NAME = "queue"
        const val EXCHANGE_NAME = "exchange"
        const val ROUTING_KEY = "dividends"
    }

    @Bean
    fun queue() = Queue(QUEUE_NAME, false)

    @Bean
    fun exchange() = TopicExchange(EXCHANGE_NAME)

    @Bean
    fun binding(
        queue: Queue,
        exchange: TopicExchange,
    ): Binding = BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY)
}
