package com.zorroa.analyst.config

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.common.config.NetworkEnvironmentUtils
import com.zorroa.common.config.SpringApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.endpoint.InfoEndpoint
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApplicationConfig {

    @Bean
    fun applicationProperties() : ApplicationProperties {
        return SpringApplicationProperties()
    }

    @Bean
    fun analyzeThreadPool(): ExecutorService {
        var threads = applicationProperties().getInt("analyst.executor.threads")
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors()
        } else if (threads < 0) {
            threads = Runtime.getRuntime().availableProcessors() / 2
        }
        System.setProperty("analyst.executor.threads", threads.toString())
        return Executors.newFixedThreadPool(threads)
    }

    @Bean
    @Throws(IOException::class)
    fun infoEndpoint(): InfoEndpoint {

        val info = InfoContributor{ builder ->
            builder.withDetail("description", "Zorroa Analyst Server")
            builder.withDetail("project", "zorroa-analyst")

            try {
                val props = Properties()
                props.load(ClassPathResource("version.properties").inputStream)
                builder.withDetail("version", props.getProperty("build.version"))
                builder.withDetail("date", props.getProperty("build.date"))
                builder.withDetail("user", props.getProperty("build.user"))
                builder.withDetail("commit", props.getProperty("build.id"))
                builder.withDetail("branch", props.getProperty("build.branch"))
                builder.withDetail("dirty", props.getProperty("build.dirty"))
            } catch (e: IOException) {
                logger.warn("Can't find info version properties")
            }
        }

        return InfoEndpoint(ImmutableList.of<InfoContributor>(info))
    }

    @Bean
    fun getNetworkEnvironment(): NetworkEnvironment {
        return NetworkEnvironmentUtils.getNetworkEnvironment("analyst", applicationProperties())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }

}


