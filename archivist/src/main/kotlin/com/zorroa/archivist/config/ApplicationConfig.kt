package com.zorroa.archivist.config

import com.google.common.collect.ImmutableList
import com.google.common.eventbus.EventBus
import com.zorroa.archivist.domain.UniqueTaskExecutor
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.filesystem.UUIDFileSystem
import com.zorroa.archivist.repository.UserDao
import com.zorroa.archivist.security.*
import com.zorroa.archivist.service.*
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.clients.*
import io.undertow.servlet.api.SecurityConstraint
import io.undertow.servlet.api.SecurityInfo
import io.undertow.servlet.api.TransportGuaranteeType
import io.undertow.servlet.api.WebResourceCollection
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.io.File
import java.io.IOException
import java.lang.IllegalStateException
import java.nio.file.Files
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
                    File("config/ext").walkTopDown()
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
                        .setConfidentialPortManager {
                            properties().getInt("server.port", 8066)
                        }
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
        val path = properties().getPath("archivist.storage.local.path").resolve("ofs")
        val ufs = UUIDFileSystem(path.toFile())
        ufs.init()
        return ufs
    }

    /**
     * Initialize the internal file storage system.  This is either "local" for a shared
     * NFS mount or "gcs" for Google Cloud Storage.
     */
    @Bean
    fun fileStorageService() : FileStorageService {
        val props = properties()
        val type = props.getString("archivist.storage.type")
        return when(type) {
            "local"-> {
                val path = properties().getPath("archivist.storage.local.path").resolve("ofs")
                logger.info("Initializing local storage: {}", path)
                val ufs = UUIDFileSystem(path.toFile())
                ufs.init()
                OfsFileStorageService(ufs)
            }
            "gcs"-> {
                logger.info("Initializing GCP storage")
                val configPath = props.getPath("archivist.config.path")
                val configFile = configPath.resolve("data-credentials.json")
                GcsFileStorageService(configFile)
            }
            else -> {
                throw IllegalStateException("Invalid storage type: $type")

            }
        }
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
        return FakeIndexRoutingServiceImpl(properties().getString("es-routing-service.url"))
    }

    @Autowired
    @Bean
    fun esClientCache(routingService: IndexRoutingService) : EsClientCache {
        return EsClientCache(routingService)
    }

    @Bean
    fun assetService() : AssetService {
        val network = networkEnvironment()
        val type = properties().getString("archivist.assetStore.type", "sql")
        logger.info("Initializing Core Asset Store: {}", type)
        return when(type) {
            "irm"-> {
                val path = properties()
                        .getPath("archivist.config.path")
                        .resolve("service-credentials.json")
                IrmAssetServiceImpl(
                    IrmCoreDataVaultClientImpl(network.getPublicUrl("core-data-vault-api"), path))
            }
            else->AssetServiceImpl()
        }
    }

    @Bean
    @Autowired
    fun jwtValidator(userDao: UserDao) : JwtValidator {
        val validators = mutableListOf<JwtValidator>()
        validators.add(UserJwtValidator(userDao))

        val path = properties().getPath("archivist.config.path")
                .resolve("service-credentials.json")
        if (Files.exists(path)) {
            validators.add(GcpJwtValidator(path.toString()))
        }
        return MasterJwtValidator(validators)
    }

    @Bean
    fun networkEnvironment(): NetworkEnvironment {
        val props = properties()
        val override = props.getMap("env.host-override")
                .map { it.key.split('.').last() to it.value.toString() }.toMap()

        logger.info("Host overrides: {}", override)

        return when (props.getString("env.type")) {
            "app-engine"->  {
                val project: String = System.getenv("GCLOUD_PROJECT")
                GoogleAppEngineEnvironment(project, override)
            }
            "static-vm"-> {
                StaticVmEnvironment(props.getString("env.project", "dev"), override)
            }
            "docker-compose" -> {
                DockerComposeEnvironment(override)
            }
            else-> {
                DockerComposeEnvironment(override)
            }
        }
    }

    @Bean
    @ConditionalOnProperty(name=["archivist.debug-mode.enabled"], havingValue = "true")
    fun requestLoggingFilter() : CommonsRequestLoggingFilter  {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(1024)
        return filter
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ArchivistConfiguration::class.java)

        var unittest = false
    }
}


