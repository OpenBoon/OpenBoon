package com.zorroa.irm.studio

import com.google.common.collect.Lists
import com.zorroa.irm.studio.rest.RestClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter

@Configuration
class ApplicationConfig {

    @Bean
    fun requestMappingHandlerAdapter(): RequestMappingHandlerAdapter {
        val adapter = RequestMappingHandlerAdapter()
        adapter.messageConverters = Lists.newArrayList<HttpMessageConverter<*>>(
                MappingJackson2HttpMessageConverter()
        )
        return adapter
    }

    /**
     * The Organiztion service currently talks to a fake or
     */
    @Bean
    fun indexRoutingService() : RestClient {
        return RestClient("http://localhost:8080")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApplicationConfig::class.java)
    }
}
