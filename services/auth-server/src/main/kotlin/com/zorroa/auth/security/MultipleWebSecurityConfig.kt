package com.zorroa.auth.security

import com.zorroa.auth.domain.ApiKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@ConfigurationProperties("security")
class SecurityProperties {

    var serviceKey: String? = null
}

@EnableWebSecurity
class MultipleWebSecurityConfig {

    @Autowired
    lateinit var securityProperties: SecurityProperties

    @Autowired
    lateinit var jwtAuthenticationProvider: JwtAuthenticationProvider

    @Configuration
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    class WebSecurityConfiguration : WebSecurityConfigurerAdapter() {

        @Autowired
        lateinit var jwtAuthorizationFilter: JWTAuthorizationFilter

        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/auth/**")
                .addFilterAfter(
                    jwtAuthorizationFilter,
                    UsernamePasswordAuthenticationFilter::class.java
                )
                .csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }
    }

    @Configuration
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    class SwaggerConfigSecurity : WebSecurityConfigurerAdapter() {

        @Value("\${swagger.isPublic}")
        var isPublic: Boolean = true

        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/**")
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable()

            if (isPublic) {
                http.authorizeRequests()
                    .antMatchers("/v2/api-docs").permitAll()
                    .antMatchers("/swagger-ui.html").permitAll()
                    .antMatchers("/error").permitAll()
            } else {
                http.authorizeRequests()
                    .antMatchers("/v2/api-docs").denyAll()
                    .antMatchers("/swagger-ui.html").denyAll()
                    .antMatchers("/error").denyAll()
            }
        }
    }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(jwtAuthenticationProvider)
    }

    @Bean
    fun serviceKey(): ApiKey {
        return loadServiceKey(securityProperties.serviceKey)
    }

    @Bean
    fun jwtAuthorizationFilter(): JWTAuthorizationFilter {
        return JWTAuthorizationFilter()
    }

    @Bean
    fun jwtKeyFilterRegistration(): FilterRegistrationBean<JWTAuthorizationFilter> {
        val bean = FilterRegistrationBean<JWTAuthorizationFilter>()
        bean.filter = jwtAuthorizationFilter()
        bean.isEnabled = false
        return bean
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSecurityConfiguration::class.java)
    }
}



