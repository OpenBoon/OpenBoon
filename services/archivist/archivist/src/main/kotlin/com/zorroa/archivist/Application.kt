package com.zorroa.archivist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [SecurityAutoConfiguration::class],
    scanBasePackages = ["com.zorroa.zmlp.service", "com.zorroa.archivist"]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
