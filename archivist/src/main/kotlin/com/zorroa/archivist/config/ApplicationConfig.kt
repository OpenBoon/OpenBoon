package com.zorroa.archivist.config

import com.google.common.collect.ImmutableList
import com.google.common.collect.Lists
import com.google.common.eventbus.EventBus
import com.zorroa.archivist.domain.UniqueTaskExecutor
import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.service.TransactionEventManager
import com.zorroa.common.config.NetworkEnvironment
import com.zorroa.common.config.NetworkEnvironmentUtils
import com.zorroa.common.config.SpringApplicationProperties
import com.zorroa.common.elastic.ElasticClientUtils
import com.zorroa.sdk.filesystem.ObjectFileSystem
import com.zorroa.sdk.filesystem.UUIDFileSystem
import com.zorroa.sdk.processor.SharedData
import com.zorroa.sdk.util.FileUtils
import io.undertow.server.HttpServerExchange
import io.undertow.servlet.api.*
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.endpoint.InfoEndpoint
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder
import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.awt.SystemColor.info
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Stream
import javax.sql.DataSource

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
class ArchivistConfiguration {

    @Bean
    fun properties() : ApplicationProperties = SpringApplicationProperties()

    @Bean
    fun getNetworkEnvironment() : NetworkEnvironment =
            NetworkEnvironmentUtils.getNetworkEnvironment("archivist", properties())

    @Bean
    @ConfigurationProperties(prefix = "archivist.datasource.primary")
    fun dataSource(): DataSource {
        return DataSourceBuilder.create().build()
    }

    @Bean
    @Autowired
    fun transactionManager(datasource: DataSource): DataSourceTransactionManager {
        return DataSourceTransactionManager(datasource)
    }

    @Bean
    fun transactionEventManager(): TransactionEventManager {
        return TransactionEventManager()
    }

    @Bean
    fun servletWebServerFactory() : UndertowEmbeddedServletContainerFactory {
        val factory =  UndertowEmbeddedServletContainerFactory()
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
                        .setConfidentialPortManager(ConfidentialPortManager {8066})
            })
        }
        return factory
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
    fun requestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
        val adapter = RequestMappingHandlerAdapter()
        adapter.messageConverters = Lists.newArrayList<HttpMessageConverter<*>>(
                MappingJackson2HttpMessageConverter()
        )
        return adapter
    }

    @Bean
    fun ofs(): ObjectFileSystem {
        val ufs = UUIDFileSystem(File(properties().getString("archivist.path.ofs")))
        ufs.init()
        return ufs
    }

    @Bean
    fun sharedData(): SharedData {
        return SharedData(properties().getString("archivist.path.shared"))
    }

    @Bean
    fun folderTaskExecutor(): UniqueTaskExecutor {
        return UniqueTaskExecutor(unittest)
    }

    @Bean
    fun eventBus() : EventBus {
        return EventBus()
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

@Configuration
class ElasticConfig {

    @Autowired
    internal lateinit var properties: ApplicationProperties

    @Bean
    @Throws(IOException::class)
    fun elastic(): Client {

        val hostName = InetAddress.getLocalHost().hostName
        val nodeName = String.format("%s_master", hostName)

        val builder = Settings.settingsBuilder()

        builder
                .put("path.data", properties.getString("archivist.path.index"))
                .put("path.home", properties.getString("archivist.path.home"))
                .put("cluster.name", "zorroa")
                .put("node.name", nodeName)
                .put("transport.tcp.port", properties.getInt("zorroa.cluster.index.port"))
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("discovery.zen.fd.ping_timeout", "3s")
                .put("discovery.zen.fd.ping_retries", 10)
                .put("script.engine.groovy.indexed.update", true)
                .put("node.data", properties.getBoolean("archivist.index.data"))
                .put("node.master", properties.getBoolean("archivist.index.master"))
                .put("path.plugins", "{path.home}/es-plugins")
                .put("action.auto_create_index", "-arch*,+.scripts,-*")
                .put("network.host", "0.0.0.0")
                .put("http.host", "0.0.0.0")
                .put("http.enabled", false)

        if (properties.getBoolean("archivist.debug-mode.enabled")) {
            logger.info("----- ARCHIVIST DEBUG MODE ACTIVATED ----- ")
            builder
                    .put("http.cors.enabled", true)
                    .put("http.cors.allow-origin", "*")
                    .put("http.enabled", true)
        }

        if (properties.getBoolean("archivist.maintenance.backups.enabled")) {
            val path = FileUtils.normalize(properties.getPath("archivist.path.backups").resolve("index"))
            builder.putArray("path.repo", path.toString())
        }

        if (ArchivistConfiguration.unittest) {
            builder.put("index.refresh_interval", "1s")
            builder.put("index.translog.disable_flush", true)
            builder.put("node.local", true)
        }

        val extConfig = properties.getPath("archivist.path.home").resolve("config/elasticsearch.yml")
        if (extConfig.toFile().exists()) {
            builder.loadFromPath(extConfig)
        }
        return ElasticClientUtils.initializeClient(builder)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ArchivistConfiguration::class.java)
    }

}

