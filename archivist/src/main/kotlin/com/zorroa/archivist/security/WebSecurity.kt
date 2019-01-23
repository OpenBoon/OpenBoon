package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.service.event
import com.zorroa.archivist.service.warnEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.security.web.access.channel.ChannelProcessingFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
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
    class LoginSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                    .antMatcher("/api/**/login")
                    .antMatcher("/api/**/login")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and().headers().frameOptions().disable()
                    .and().httpBasic()
                    .and().csrf().csrfTokenRepository(csrfTokenRepository)
                    .requireCsrfProtectionMatcher(csrfRequestMatcher)

            if (properties.getBoolean("archivist.debug-mode.enabled")) {
                http.authorizeRequests()
                        .requestMatchers(RequestMatcher { CorsUtils.isCorsRequest(it) }).permitAll()
                        .and().addFilterBefore(CorsCredentialsFilter(), ChannelProcessingFilter::class.java)
            }
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Bean(name=["globalAuthenticationManager"])
        @Throws(Exception::class)
        fun globalAuthenticationManager(): AuthenticationManager {
            return super.authenticationManagerBean()
        }

        @Bean
        fun resetPasswordSecurityFilter(): ResetPasswordSecurityFilter {
            return ResetPasswordSecurityFilter()
        }

        @Bean
        fun jwtAuthorizationFilter() : JWTAuthorizationFilter {
            return JWTAuthorizationFilter(globalAuthenticationManager())
        }

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                    .antMatcher("/api/**")
                    .addFilterBefore(jwtAuthorizationFilter(), CsrfFilter::class.java)
                    .addFilterBefore(resetPasswordSecurityFilter(), UsernamePasswordAuthenticationFilter::class.java)
                    .authorizeRequests()
                    .antMatchers("/api/v1/logout").permitAll()
                    .antMatchers("/api/v1/who").permitAll()
                    .antMatchers("/api/v1/reset-password").permitAll()
                    .antMatchers("/api/v1/send-password-reset-email").permitAll()
                    .antMatchers("/api/v1/send-onboard-email").permitAll()
                    .anyRequest().authenticated()
                    .and().headers().frameOptions().disable().cacheControl().disable()
                    .and().csrf().csrfTokenRepository(csrfTokenRepository)
                    .requireCsrfProtectionMatcher(csrfRequestMatcher)
                    .and()
                    .exceptionHandling().authenticationEntryPoint {
                        _: HttpServletRequest, rsp: HttpServletResponse, exp: AuthenticationException ->
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
        lateinit var analystAuthenticationFilter : AnalystAuthenticationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                    .antMatcher("/cluster/**")
                    .addFilterBefore(analystAuthenticationFilter, CsrfFilter::class.java)
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

        @Autowired
        internal lateinit var jwtAuthorizationFilter : JWTAuthorizationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                    .antMatcher("/actuator/**")
                    .addFilterBefore(jwtAuthorizationFilter, CsrfFilter::class.java)
                    .authorizeRequests()
                    .requestMatchers(EndpointRequest.to("metrics", "prometheus")).hasAuthority("zorroa::monitor")
                    .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
        }
    }


    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {

        auth
                .authenticationProvider(jwtAuthenticationProvider())
                .authenticationProvider(zorroaAuthenticationProvider())
                .authenticationEventPublisher(authenticationEventPublisher())

        /**
         * If its a unit test we add our rubber stamp authenticator.
         */
        if (ArchivistConfiguration.unittest) {
            auth.authenticationProvider(UnitTestAuthenticationProvider())
        }
    }

    @Bean
    fun authenticationEventPublisher(): AuthenticationEventPublisher {

        return object : AuthenticationEventPublisher {

            override fun publishAuthenticationSuccess(authentication: Authentication) {
                try {
                    // TODO Move this into different auth plugin
                    authentication.principal?.let {
                        logger.event(LogObject.USER, LogAction.AUTHENTICATE,
                                mapOf("principal" to it))
                    }

                } catch (e: Exception) {
                    logger.warn("Failed to log user authentication", e)
                }
            }

            override fun publishAuthenticationFailure(
                    exception: AuthenticationException,
                    authentication: Authentication) {

                if (properties.getBoolean("archivist.debug-mode.enabled")) {
                    logger.warnEvent(LogObject.USER, LogAction.ERROR,
                            "failed to authenticate", emptyMap(), exception)
                }

            }
        }
    }

    @Bean
    fun zorroaAuthenticationProvider(): ZorroaAuthenticationProvider {
        return ZorroaAuthenticationProvider()
    }

    /**
     * An AuthenticationProvider that handles previously validated JWT claims.
     */
    @Bean
    fun jwtAuthenticationProvider(): JwtAuthenticationProvider {
        return JwtAuthenticationProvider()
    }

    companion object {

        /**
         * an Http RequestMatcher for requiring CSRF protection
         */
        val csrfRequestMatcher = RequestMatcher {
            if (it.getAttribute("authType") == HttpServletRequest.CLIENT_CERT_AUTH) {
                false
            }
            else {
                it.method !in setOf("GET", "HEAD", "TRACE", "OPTIONS")
            }
        }

        private val csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()

        private val logger = LoggerFactory.getLogger(MultipleWebSecurityConfig::class.java)
    }
}


