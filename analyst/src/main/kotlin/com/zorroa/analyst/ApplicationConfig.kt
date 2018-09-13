package com.zorroa.analyst

import com.zorroa.analyst.scheduler.K8SchedulerProperties
import com.zorroa.analyst.scheduler.K8SchedulerServiceImpl
import com.zorroa.analyst.scheduler.LocalSchedulerServiceImpl
import com.zorroa.analyst.service.*
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.server.*
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.nio.file.Files
import java.nio.file.Paths

@Configuration
@ConfigurationProperties("env")
class NetworkEnvironmentProperties {
    var type: String = "dev"
    var project : String = "dev"
    var hostOverride: Map<String,String> = mapOf()
}


@Configuration
class ApplicationConfig {

    @Autowired
    lateinit var storageProperties: StorageProperties

    @Autowired
    lateinit var schedulerProperties: SchedulerProperties

    @Value("\${analyst.config.path}")
    lateinit var configPath: String

    @Bean
    fun storageService() : JobStorageService {
        logger.info("Initializing {} storage service", storageProperties.type)
        return when (storageProperties.type) {
            "gcp"-> GcpStorageServiceImpl()
            else-> LocalJobStorageServiceImpl()
        }
    }

    @Bean
    fun schedulerService() : SchedulerService {
        return when (schedulerProperties.type) {
            "k8"-> {
                val k8props = Json.Mapper.convertValue(
                        schedulerProperties.k8, K8SchedulerProperties::class.java)
                K8SchedulerServiceImpl(k8props)
            }
            else-> LocalSchedulerServiceImpl()
        }
    }

    @Bean
    fun requestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
        val adapter = RequestMappingHandlerAdapter()
        adapter.messageConverters = mutableListOf<HttpMessageConverter<*>>(
                MappingJackson2HttpMessageConverter()
        )
        return adapter
    }

    @Bean
    fun jwtValidator() : JwtValidator {
        val path = "$configPath/service-credentials.json"
        return if (Files.exists(Paths.get(path))) {
            GcpJwtValidator(path)
        }
        else {
            NoOpJwtValidator()
        }
    }

    @Bean
    @Autowired
    fun networkEnvironment(props : NetworkEnvironmentProperties): NetworkEnvironment {
        logger.info("Initializing network env: ${props.type}, overrides: ${props.hostOverride}")

        return when (props.type) {
            "app-engine"->  {
                val project: String = System.getenv("GCLOUD_PROJECT")
                GoogleAppEngineEnvironment(project, props.hostOverride)
            }
            "static-vm"-> {
                StaticVmEnvironment(props.project, props.hostOverride)
            }
            "docker-compose" -> {
                DockerComposeEnvironment(props.hostOverride)
            }
            else-> {
                DockerComposeEnvironment(props.hostOverride)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}
