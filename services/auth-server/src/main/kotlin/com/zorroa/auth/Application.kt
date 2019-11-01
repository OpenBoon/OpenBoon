package com.zorroa.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class ZorroaAuthServerApplication

fun main(args: Array<String>) {
    runApplication<ZorroaAuthServerApplication>(*args)
}
