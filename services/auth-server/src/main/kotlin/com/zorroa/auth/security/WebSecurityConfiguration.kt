package com.zorroa.auth.security

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.Role
import com.zorroa.auth.service.KeyGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.util.*

@Configuration
@ConfigurationProperties("security")
class SecurityProperties {

    var adminKey: Resource? = null
}

@Configuration
@EnableWebSecurity
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
                .antMatchers("/auth/v1/apikey*").hasRole(Role.SUPERADMIN_PERM)
                .antMatchers("/v2/api-docs").hasRole(Role.SUPERADMIN_PERM)
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
        securityProperties.adminKey?.let {
            val mapper = jacksonObjectMapper()
            return mapper.readValue(it.inputStream, ApiKey::class.java)
        }

        // Otherwise return a random key that is impossible to use.
        return ApiKey(
                UUID.randomUUID(),
                UUID.randomUUID(),
                KeyGenerator.generate(),
                "random")
    }

    @Bean
    fun jwtAuthorizationFilter(): JWTAuthorizationFilter {
        return JWTAuthorizationFilter()
    }

}