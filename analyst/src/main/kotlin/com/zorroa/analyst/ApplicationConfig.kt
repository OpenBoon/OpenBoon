package com.zorroa.analyst

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.common.collect.Lists
import com.zorroa.analyst.security.JwtCredentials
import com.zorroa.analyst.service.*
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.service.CoreDataVaultService
import com.zorroa.common.service.IrmCoreDataVaultServiceImpl
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@Configuration
class ApplicationConfig {

    @Value("\${cdv.url}")
    lateinit var cdvUrl: String

    @Autowired
    lateinit var storageProperties: StorageProperties

    @Autowired
    lateinit var schedulerProperties: SchedulerProperties

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
        adapter.messageConverters = Lists.newArrayList<HttpMessageConverter<*>>(
                MappingJackson2HttpMessageConverter()
        )
        return adapter
    }

    @Bean
    fun routingService() : IndexRoutingService {
        return IndexRoutingServiceImpl()
    }

    @Bean
    fun jwtCredential() : JwtCredentials {
        val path = "config/credentials.json"
        return if (Files.exists(Paths.get(path))) {
            val cred = GoogleCredential.fromStream(FileInputStream(path))
                JwtCredentials(String(Base64.getEncoder().encode(cred.serviceAccountPrivateKey.encoded)))
            }
        else {
            logger.warn("Unable to find credentials file, defaulting to random key")
            JwtCredentials(UUID.randomUUID().toString())
        }
    }

    @Autowired
    @Bean
    fun esClientCache(routingService: IndexRoutingService) : EsClientCache {
        return EsClientCache(routingService)
    }

    @Bean
    fun coreDataVault() : CoreDataVaultService {
        return IrmCoreDataVaultServiceImpl(cdvUrl)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}
