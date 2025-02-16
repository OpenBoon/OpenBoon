package boonai.archivist.config

import boonai.archivist.service.EsClientCache
import boonai.archivist.service.TransactionEventManager
import boonai.archivist.util.FileUtils
import boonai.common.service.security.EncryptionService
import boonai.common.service.security.EncryptionServiceImpl
import com.google.common.collect.ImmutableList
import com.google.common.eventbus.EventBus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.io.File
import java.io.IOException
import java.util.Properties

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ArchivistConfiguration {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Bean
    fun transactionEventManager(): TransactionEventManager {
        return TransactionEventManager()
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

            builder.withDetail(
                "archivist-plugins",
                File("/extensions/active").walkTopDown()
                    .map { it.toString() }
                    .filter { it.endsWith(".jar") }
                    .map { FileUtils.basename(it) }
                    .toList()
            )
        }

        return InfoEndpoint(ImmutableList.of(info))
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

    @Bean
    fun encryptionService(): EncryptionService {
        return EncryptionServiceImpl()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ArchivistConfiguration::class.java)

        var unittest = false
    }
}

@Configuration
class MessageServiceConfiguration {

    @Bean
    fun requestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
        val adapter = RequestMappingHandlerAdapter()
        adapter.messageConverters = listOf<HttpMessageConverter<*>>(
            MappingJackson2HttpMessageConverter()
        )
        return adapter
    }
}
