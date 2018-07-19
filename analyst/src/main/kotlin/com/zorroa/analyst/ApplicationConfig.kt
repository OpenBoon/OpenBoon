package com.zorroa.analyst

import com.google.common.collect.Lists
import com.zorroa.common.clients.RestClient
import com.zorroa.common.service.CoreDataVaultService
import com.zorroa.common.service.IrmCoreDataVaultServiceImpl
import org.slf4j.LoggerFactory
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

    @Bean
    fun requestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
        val adapter = RequestMappingHandlerAdapter()
        adapter.messageConverters = Lists.newArrayList<HttpMessageConverter<*>>(
                MappingJackson2HttpMessageConverter()
        )
        return adapter
    }

    /**
     * The Organization service currently talks to a fake service
     */
    @Bean
    fun indexRoutingService() : RestClient {
        return RestClient("http://localhost:8080")
    }

    @Bean
    fun coreDataVault() : CoreDataVaultService {
        return IrmCoreDataVaultServiceImpl(cdvUrl)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}
