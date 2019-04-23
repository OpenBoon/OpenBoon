package com.zorroa.security.saml

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@ComponentScan("com.zorroa.security.saml")
@Configuration
class MvcConfig : WebMvcConfigurer {

    @Autowired
    lateinit var currentUserHandlerMethodArgumentResolver: HandlerMethodArgumentResolver

    override fun addArgumentResolvers(argumentResolvers: MutableList<HandlerMethodArgumentResolver>) {
        argumentResolvers.add(currentUserHandlerMethodArgumentResolver)
    }

}


