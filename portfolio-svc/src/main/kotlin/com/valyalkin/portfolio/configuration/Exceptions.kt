package com.valyalkin.portfolio.configuration

class NotFoundException(
    override val message: String,
) : RuntimeException(message)

class BusinessException(
    override val message: String,
) : RuntimeException(message)

class SystemException(
    override val message: String,
) : RuntimeException(message)
