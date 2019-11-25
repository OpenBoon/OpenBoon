package com.zorroa.archivist.config

import com.google.common.collect.ImmutableList
import com.google.common.eventbus.EventBus
import com.zorroa.archivist.service.EsClientCache
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileServerProviderImpl
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.GcsFileStorageService
import com.zorroa.archivist.service.LocalFileStorageService
import com.zorroa.archivist.service.MessagingService
import com.zorroa.archivist.service.NullMessagingService
import com.zorroa.archivist.service.PubSubMessagingService
import com.zorroa.archivist.service.TransactionEventManager
import com.zorroa.archivist.util.FileUtils
import io.sentry.spring.SentryExceptionResolver
import io.sentry.spring.SentryServletContextInitializer
import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.Properties

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ArchivistConfiguration {

    @Bean
    fun properties(): ApplicationProperties = SpringApplicationProperties()

    @Bean
    fun transactionEventManager(): TransactionEventManager {
        return TransactionEventManager()
    }

    @Bean
    fun redisClient(): JedisPool {
        val props = properties()
        val host = props.getString("archivist.redis.host")
        val port = props.getInt("archivist.redis.port")
        return JedisPool(JedisPoolConfig(), host, port, 10000)
    }

    @Bean
    fun esClientCache(): EsClientCache {
        return EsClientCache()
    }

    @Bean
    @Throws(IOException::class)
    fun infoEndpoint(): InfoEndpoint {
        val info = InfoContributor { builder ->
            builder.withDetail("description", "Zorroa Archivist Server")
            builder.withDetail("project", "zorroa-archivist")

            try {
                val props = Properties()
                props.load(ClassPathResource("version.properties").inputStream)
                props.stringPropertyNames().forEach {
                    builder.withDetail(it, props.getProperty(it))
                }
            } catch (e: IOException) {
                logger.warn("Can't find info version properties")
            }

            builder.withDetail("archivist-plugins",
                File("/extensions/active").walkTopDown()
                    .map { it.toString() }
                    .filter { it.endsWith(".jar") }
                    .map { FileUtils.basename(it) }
                    .toList())
        }

        return InfoEndpoint(ImmutableList.of(info))
    }

    @Bean
    fun requestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
        val adapter = RequestMappingHandlerAdapter()
        adapter.messageConverters = listOf<HttpMessageConverter<*>>(
            MappingJackson2HttpMessageConverter()
        )
        return adapter
    }

    @Bean
    fun fileServerProvider(): FileServerProvider {
        return FileServerProviderImpl(properties(), dataCredentials())
    }

    /**
     * Initialize the internal file storage system.  This is either "local" for a shared
     * NFS mount or "gcs" for Google Cloud Storage.
     */
    @Bean
    fun fileStorageService(): FileStorageService {
        val props = properties()
        val type = props.getString("archivist.storage.type")
        return when (type) {
            "local" -> {
                val path = properties().getPath("archivist.storage.path")
                // OFS gets shoved into the OFS dir.
                LocalFileStorageService(path)
            }
            "gcs" -> {
                val bucket = properties().getString("archivist.storage.bucket")
                GcsFileStorageService(bucket, dataCredentials())
            }
            else -> {
                throw IllegalStateException("Invalid storage type: $type")
            }
        }
    }

    @Bean
    fun messagingService(): MessagingService {
        val props = properties()
        val type = props.getString("archivist.messaging-service.type", "None")
        return when (type) {
            "pubsub" -> {
                logger.info("Using Pub/Sub messaging service.")
                PubSubMessagingService(
                    topicId = props.getString("archivist.messaging-service.topicId", "archivist-events")
                )
            }
            else -> {
                NullMessagingService()
            }
        }
    }

    @Bean
    fun workQueue(): AsyncListenableTaskExecutor {
        val tpe = ThreadPoolTaskExecutor()
        tpe.corePoolSize = 8
        tpe.maxPoolSize = 8
        tpe.threadNamePrefix = "WORK-QUEUE-"
        tpe.isDaemon = true
        tpe.setQueueCapacity(1000)
        return tpe
    }

    @Bean
    fun eventBus(): EventBus {
        return EventBus()
    }

    /**
     * The service credentials key.
     */
    fun serviceCredentials(): Path {
        return properties()
            .getPath("archivist.config.path")
            .resolve("service-credentials.json")
    }

    /**
     *  The data credentials key.
     */
    fun dataCredentials(): Path {
        return properties()
            .getPath("archivist.config.path")
            .resolve("data-credentials.json")
    }

    @Bean
    fun sentryExceptionResolver(): HandlerExceptionResolver = SentryExceptionResolver()

    @Bean
    fun sentryServletContextInitializer(): ServletContextInitializer = SentryServletContextInitializer()

    companion object {

        private val logger = LoggerFactory.getLogger(ArchivistConfiguration::class.java)

        var unittest = false
    }
}
