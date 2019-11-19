package com.zorroa.archivist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import io.sentry.spring.SentryExceptionResolver
import io.sentry.spring.SentryServletContextInitializer
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean


@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class Application {
    @Bean
    fun sentryExceptionResolver(): HandlerExceptionResolver = SentryExceptionResolver()

    @Bean
    fun sentryServletContextInitializer(): ServletContextInitializer = SentryServletContextInitializer()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
