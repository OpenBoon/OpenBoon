package com.zorroa.auth

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

val JSON_MAPPER = jacksonObjectMapper()

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
class ZorroaAuthServerApplication

fun main(args: Array<String>) {
    runApplication<ZorroaAuthServerApplication>(*args)
}
