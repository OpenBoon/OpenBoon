package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.Groups
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.service.warnEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.access.channel.ChannelProcessingFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsUtils
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * MultipleWebSecurityConfig sets up all the endpoint security.
 *
 * The secret to doing this is that the configure method in each WebSecurityConfigurerAdapter
 * must start off with a .antMatcher(pattern) function.  Each WebSecurityConfigurerAdapter
 * instance handles configuring a different groups of endpoints.
 *
 * Warning: using this setting messes up tests.
 * SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
 */
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
class MultipleWebSecurityConfig {

    @Autowired
    internal lateinit var properties: ApplicationProperties

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Bean(name = ["globalAuthenticationManager"])
        @Throws(Exception::class)
        fun globalAuthenticationManager(): AuthenticationManager {
            return super.authenticationManagerBean()
        }

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/api/**")
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/v1/logout").permitAll()
                .antMatchers("/api/v1/who").permitAll()
                .antMatchers("/api/v1/reset-password").permitAll()
                .antMatchers("/api/v1/send-password-reset-email").permitAll()
                .antMatchers("/api/v1/send-onboard-email").permitAll()
                .antMatchers("/api/v1/auth/token").permitAll()
                .anyRequest().authenticated()
                .and().headers().frameOptions().disable().cacheControl().disable()
                .and().csrf().disable()
                .exceptionHandling()
                .authenticationEntryPoint { _: HttpServletRequest, rsp: HttpServletResponse, exp: AuthenticationException ->
                    rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, exp.message)
                }

            if (properties.getBoolean("archivist.debug-mode.enabled")) {
                http.authorizeRequests()
                    .requestMatchers(RequestMatcher { CorsUtils.isCorsRequest(it) }).permitAll()
                    .and().addFilterBefore(CorsCredentialsFilter(), ChannelProcessingFilter::class.java)
            }
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class WorkerSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Autowired
        lateinit var analystAuthenticationFilter: AnalystAuthenticationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/cluster/**")
                .addFilterBefore(analystAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().hasAuthority("ANALYST")
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 3)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class ActuatorSecurityConfig : WebSecurityConfigurerAdapter() {

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/actuator/**")
                .httpBasic()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
                .hasAnyAuthority(Groups.SUPERADMIN, Groups.MONITOR)
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
        }
    }

    @Configuration
    @Order(Ordered.LOWEST_PRECEDENCE)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class RootSecurityConfig : WebSecurityConfigurerAdapter() {

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/**")
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/v2/api-docs").authenticated()
                .antMatchers("/error").permitAll()
                .antMatchers("/download-zsdk").permitAll()
                .and()
                .csrf().disable()
        }
    }

    @Value("\${management.endpoints.password}")
    lateinit var monitorPassword: String

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth
            .authenticationEventPublisher(authenticationEventPublisher())
            .inMemoryAuthentication()
            .withUser("monitor").password(passwordEncoder().encode(monitorPassword))
            .authorities(Groups.MONITOR)

        /**
         * If its a unit test we add our rubber stamp authenticator.
         */
        if (ArchivistConfiguration.unittest) {
            auth.authenticationProvider(UnitTestAuthenticationProvider())
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationEventPublisher(): AuthenticationEventPublisher {

        return object : AuthenticationEventPublisher {

            override fun publishAuthenticationSuccess(authentication: Authentication) {}
            override fun publishAuthenticationFailure(
                exception: AuthenticationException,
                authentication: Authentication
            ) {

                if (properties.getBoolean("archivist.debug-mode.enabled")) {
                    logger.warnEvent(
                        LogObject.USER, LogAction.ERROR,
                        "failed to authenticate", emptyMap(), exception
                    )
                }
            }
        }
    }

    companion object {

        /**
         * an Http RequestMatcher for requiring CSRF protection
         */
        val csrfRequestMatcher = RequestMatcher {
            if (it.getAttribute("authType") == HttpServletRequest.CLIENT_CERT_AUTH) {
                false
            } else if (it.requestURI == "/api/v1/auth/token") {
                false
            } else {
                it.method !in setOf("GET", "HEAD", "TRACE", "OPTIONS")
            }
        }

        private val csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()

        private val logger = LoggerFactory.getLogger(MultipleWebSecurityConfig::class.java)
    }
}
