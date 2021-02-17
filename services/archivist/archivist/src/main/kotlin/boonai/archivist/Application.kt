package boonai.archivist

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    exclude = [SecurityAutoConfiguration::class],
    scanBasePackages = ["boonai.common.service", "boonai.archivist"]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
