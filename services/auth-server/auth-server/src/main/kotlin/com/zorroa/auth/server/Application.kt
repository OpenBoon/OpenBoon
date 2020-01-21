package com.zorroa.auth.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@ComponentScan(value = ["com.zorroa.auth.server", "com.zorroa.zmlp.service"])
@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
