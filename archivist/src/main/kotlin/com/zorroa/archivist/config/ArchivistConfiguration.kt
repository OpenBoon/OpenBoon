package com.zorroa.archivist.config

import com.google.common.collect.ImmutableList
import com.google.common.eventbus.EventBus
import com.zorroa.archivist.filesystem.UUIDFileSystem
import com.zorroa.archivist.repository.UserDao
import com.zorroa.archivist.sdk.security.UserRegistryService
import com.zorroa.archivist.security.GcpJwtValidator
import com.zorroa.archivist.security.IrmJwtValidator
import com.zorroa.archivist.security.JwtValidator
import com.zorroa.archivist.security.MasterJwtValidator
import com.zorroa.archivist.security.UserJwtValidator
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileServerProviderImpl
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.GcpPubSubServiceImpl
import com.zorroa.archivist.service.GcsFileStorageService
import com.zorroa.archivist.service.LocalFileStorageService
import com.zorroa.archivist.service.PubSubService
import com.zorroa.archivist.service.TransactionEventManager
import com.zorroa.archivist.util.FileUtils
import com.zorroa.common.clients.IrmCoreDataVaultClientImpl
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.core.io.ClassPathResource
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
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
                val ufs = UUIDFileSystem(path.resolve("ofs"))
                LocalFileStorageService(path, ufs)
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
    @Autowired
    fun pubSubService(meterRegistry: MeterRegistry): PubSubService? {
        val props = properties()
        if (props.getString("archivist.pubsub.type") == "gcp") {
            val network = networkEnvironment()
            return GcpPubSubServiceImpl(
                IrmCoreDataVaultClientImpl(
                    network.getPublicUrl("core-data-vault-api"),
                    serviceCredentials(),
                    dataCredentials(), meterRegistry
                ), meterRegistry
            )
        }
        logger.info("No PubSub service configured")
        return null
    }

    @Bean
    @Autowired
    fun masterJwtValidator(userRegistryService: UserRegistryService, userDao: UserDao): MasterJwtValidator {
        val validators = mutableListOf<JwtValidator>()
        val props = properties()
        val jwtModules = props.getList("archivist.security.jwt.modules")

        /**
         * Determine which JWT validators to instance.
         */
        for (module in jwtModules) {
            val moduleInstance = when (module) {
                "zorroa" -> {
                    UserJwtValidator(userDao)
                }
                "irm" -> {
                    IrmJwtValidator(
                        props.getPath("archivist.security.jwt.irm.credentials-path", serviceCredentials()),
                        props.parseToMap("archivist.security.permissions.map"), userRegistryService
                    )
                }
                "gcp" -> {
                    GcpJwtValidator(props.getPath("archivist.security.jwt.gcp.credentials-path", serviceCredentials()))
                }
                else -> {
                    throw IllegalArgumentException("Invalid jwt module: $module")
                }
            }
            validators.add(moduleInstance)
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
            "app-engine" -> {
                val project: String = System.getenv("GCLOUD_PROJECT")
                GoogleAppEngineEnvironment(project, override)
            }
            "static-vm" -> {
                StaticVmEnvironment(props.getString("env.project", "dev"), override)
            }
            "docker-compose" -> {
                DockerComposeEnvironment(override)
            }
            else -> {
                DockerComposeEnvironment(override)
            }
        }
    }

    @Bean
    @ConditionalOnProperty(name = ["archivist.debug-mode.enabled"], havingValue = "true")
    fun requestLoggingFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludePayload(true)
        filter.setMaxPayloadLength(1024)
        return filter
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

    companion object {

        private val logger = LoggerFactory.getLogger(ArchivistConfiguration::class.java)

        var unittest = false
    }
}
