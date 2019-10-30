package com.zorroa.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer

@EnableAuthorizationServer
@SpringBootApplication
class ZorroaAuthServerApplication

fun main(args: Array<String>) {
	runApplication<ZorroaAuthServerApplication>(*args)
}
