package com.zorroa.archivist.config

import com.google.common.collect.ImmutableList
import com.google.common.eventbus.EventBus
import com.zorroa.archivist.domain.UniqueTaskExecutor
import com.zorroa.archivist.service.GcpStorageService
import com.zorroa.archivist.service.StorageService
import com.zorroa.archivist.service.TransactionEventManager
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.FakeIndexRoutingServiceImpl
import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.filesystem.ObjectFileSystem
import com.zorroa.common.filesystem.UUIDFileSystem
import com.zorroa.common.util.FileUtils
import io.undertow.servlet.api.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.io.File
import java.io.IOException
import java.util.*

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ArchivistConfiguration {

    @Bean
    fun properties() : ApplicationProperties = SpringApplicationProperties()

    @Bean
    fun transactionEventManager(): TransactionEventManager {
        return TransactionEventManager()
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
                    File("lib/ext").walkTopDown()
                    .map { it.toString() }
                    .filter { it.endsWith(".jar")}
                    .map { FileUtils.basename(it) }
                    .toList())
        }

        return InfoEndpoint(ImmutableList.of(info))
    }

    @Bean
    fun servletWebServerFactory() : UndertowServletWebServerFactory {
        val factory =  UndertowServletWebServerFactory()
        factory.addBuilderCustomizers( UndertowBuilderCustomizer {
            it.addHttpListener(properties().getInt("server.http_port", 8080), "0.0.0.0")
        })
        if (properties().getBoolean("security.require_ssl", false)) {
            factory.addDeploymentInfoCustomizers(UndertowDeploymentInfoCustomizer {
                it.addSecurityConstraint(SecurityConstraint()
                        .addWebResourceCollection(WebResourceCollection()
                                .addUrlPattern("/*"))
                        .setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL)
                        .setEmptyRoleSemantic(SecurityInfo.EmptyRoleSemantic.PERMIT))
                        .setConfidentialPortManager(ConfidentialPortManager {
                            properties().getInt("server.port", 8066)
                        })
            })
        }
        return factory
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
    fun ofs(): ObjectFileSystem {
        val ufs = UUIDFileSystem(File(properties().getString("archivivst.storage.ofs.path")))
        ufs.init()
        return ufs
    }

    /**
     * Just hard coding this for now.  In the future we'll check a configuration
     * option or load a plugin.
     */
    @Bean
    fun storageService() : StorageService {
        return GcpStorageService(properties())
    }

    @Bean
    fun folderTaskExecutor(): UniqueTaskExecutor {
        return UniqueTaskExecutor(unittest)
    }

    @Bean
    fun eventBus() : EventBus {
        return EventBus()
    }

    @Bean
    fun routingService() : IndexRoutingService {
        return FakeIndexRoutingServiceImpl(properties().getString("routing.url"))
    }

    @Autowired
    @Bean
    fun esClientCache(routingService: IndexRoutingService) : EsClientCache {
        return EsClientCache(routingService)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ArchivistConfiguration::class.java)

        var unittest = false

        fun <T> instantiate(className: String, type: Class<T>): T? {
            try {
                return type.cast(Class.forName(className).newInstance())
            } catch (e: InstantiationException) {
                throw IllegalStateException(e)
            } catch (e: IllegalAccessException) {
                throw IllegalStateException(e)
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException(e)
            }

        }
    }
}


