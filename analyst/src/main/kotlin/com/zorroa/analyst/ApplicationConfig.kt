package com.zorroa.analyst

import com.google.common.collect.Lists
import com.zorroa.analyst.service.*
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.FakeIndexRoutingServiceImpl
import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.clients.RestClient
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

@Configuration
class ApplicationConfig {

    @Value("\${cdv.url}")
    lateinit var cdvUrl: String

    @Autowired
    lateinit var storageProperties: StorageProperties

    @Autowired
    lateinit var schedulerProperties: SchedulerProperties

    @Bean
    fun storageService() : StorageService {
        return when (storageProperties.type) {
            "gcp"-> GcpStorageServiceImpl()
            else-> LocalStorageServiceImpl()
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
