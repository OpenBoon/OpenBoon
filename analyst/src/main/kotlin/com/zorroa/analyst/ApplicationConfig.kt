package com.zorroa.analyst

import com.zorroa.analyst.service.*
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.server.GcpJwtValidator
import com.zorroa.common.server.JwtValidator
import com.zorroa.common.server.NoOpJwtValidator
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.nio.file.Files
import java.nio.file.Paths

@Configuration
class ApplicationConfig {

    @Autowired
    lateinit var storageProperties: StorageProperties

    @Autowired
    lateinit var schedulerProperties: SchedulerProperties

    @Value("\${archivist.config.path}")
    lateinit var configPath: String

    @Bean
    fun storageService() : JobStorageService {
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
    fun routingService() : IndexRoutingService {
        return IndexRoutingServiceImpl()
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

    @Autowired
    @Bean
    fun esClientCache(routingService: IndexRoutingService) : EsClientCache {
        return EsClientCache(routingService)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}
