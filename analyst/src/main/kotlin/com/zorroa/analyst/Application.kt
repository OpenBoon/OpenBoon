package com.zorroa.analyst

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.system.ApplicationPidFileWriter
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.env.SimpleCommandLinePropertySource

@ComponentScan
@EnableAutoConfiguration
@SpringBootApplication
@EnableConfigurationProperties
class Application

fun main(args: Array<String>) {
    val ps = SimpleCommandLinePropertySource(*args)
    val app = SpringApplication(Application::class.java)

    if (ps.getProperty("pidfile") != null) {
        app.addListeners(ApplicationPidFileWriter(ps.getProperty("pidfile") as String))
    }
    app.run(*args)
}

val isUnitTest: Boolean get() = java.lang.Boolean.parseBoolean(System.getProperty("zorroa.unittest"))

