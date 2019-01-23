package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Configuration
import java.io.File
import java.nio.charset.Charset

@Configuration
class MonitorAuthConfig(val properties: ApplicationProperties, val userService: UserService) :
    ApplicationListener<ApplicationReadyEvent> {

    fun writeMonitorKeyFile() {
        try {
            val username = properties.getString("archivist.monitor.username", "monitor")
            val token = generateUserToken(userService.getApiKey(username))

            val pathname = properties.getString("archivist.monitor.key.path", "/monitoring/monitor.auth.key")
            val file = File(pathname)

            file.parentFile?.mkdirs()

            file.writeBytes(token.toByteArray(Charset.forName("UTF-8")))
        } catch (e: Exception) {
            logger.error("Could not write monitoring key file: ", e)
        }
    }

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        writeMonitorKeyFile()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MonitorAuthConfig::class.java)
    }
}

