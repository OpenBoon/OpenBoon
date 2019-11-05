package com.zorroa.auth.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.service.KeyGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.util.*

@Configuration
@ConfigurationProperties("security")
class SecurityProperties {

    var externalKey: Resource? = null
}

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
class WebSecurityConfiguration : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var securityProperties: SecurityProperties

    @Autowired
    lateinit var jwtAuthenticationProvider: JwtAuthenticationProvider

    override fun configure(http: HttpSecurity) {
        http
                .addFilterBefore(jwtAuthorizationFilter(),
                        UsernamePasswordAuthenticationFilter::class.java)
                .csrf().disable()
                .authorizeRequests()
                .antMatchers("/v2/api-docs").hasAuthority("MonitorServer")
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
    }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(jwtAuthenticationProvider)
    }

    @Bean
    fun externalApiKey(): ApiKey {
        securityProperties.externalKey?.let {
            val mapper = jacksonObjectMapper()
            val key = mapper.readValue(it.inputStream, ApiKey::class.java)
            logger.info("loading keyId: ${key.keyId}")
            return key
        }

        // Otherwise return a random key that is impossible to use.
        logger.warn("extenral key file not found, generating random key.")
        return ApiKey(
                UUID.randomUUID(),
                UUID.randomUUID(),
                KeyGenerator.generate(),
                "random", listOf())
    }

    @Bean
    fun jwtAuthorizationFilter(): JWTAuthorizationFilter {
        return JWTAuthorizationFilter()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSecurityConfiguration::class.java)

    }

}