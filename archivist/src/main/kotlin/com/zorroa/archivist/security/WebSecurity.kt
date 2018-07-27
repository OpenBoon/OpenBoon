package com.zorroa.archivist.security

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.UserLogSpec
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.service.EventLogService
import com.zorroa.archivist.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationEventPublisher
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.access.channel.ChannelProcessingFilter
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.cors.CorsUtils

@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
class MultipleWebSecurityConfig {

    @Autowired
    internal lateinit var properties: ApplicationProperties

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    class LoginSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                    .antMatcher("/api/**/login")
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and().headers().frameOptions().disable()
                    .and().sessionManagement()
                    .and().httpBasic()
                    .and().csrf().disable()

            if (properties.getBoolean("archivist.debug-mode.enabled")) {
                http.authorizeRequests()
                        .requestMatchers(RequestMatcher { CorsUtils.isCorsRequest(it) }).permitAll()
                        .and().addFilterBefore(CorsCredentialsFilter(), ChannelProcessingFilter::class.java)
            }
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Bean
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
                    .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter::class.java)
                    .addFilterBefore(HmacSecurityFilter(
                            properties.getBoolean("archivist.security.hmac.enabled")), UsernamePasswordAuthenticationFilter::class.java)
                    .addFilterAfter(resetPasswordSecurityFilter(), HmacSecurityFilter::class.java)
                    .authorizeRequests()
                    .antMatchers("/api/v1/logout").permitAll()
                    .antMatchers("/api/v1/who").permitAll()
                    .antMatchers("/api/v1/reset-password").permitAll()
                    .antMatchers("/api/v1/send-password-reset-email").permitAll()
                    .antMatchers("/api/v1/send-onboard-email").permitAll()
                    .requestMatchers(RequestMatcher { CorsUtils.isCorsRequest(it) }).permitAll()
                    .anyRequest().authenticated()
                    .and().headers().frameOptions().disable()
                    .and().sessionManagement()
                    .and().csrf().disable()

            if (properties.getBoolean("archivist.debug-mode.enabled")) {
                http.authorizeRequests()
                        .requestMatchers(RequestMatcher { CorsUtils.isCorsRequest(it) }).permitAll()
                        .and().addFilterBefore(CorsCredentialsFilter(), ChannelProcessingFilter::class.java)
            }
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    class FormSecurityConfig : WebSecurityConfigurerAdapter() {

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                    .antMatcher("/")
                    .csrf().disable()
                    .sessionManagement()
                    .invalidSessionUrl("/")
        }
    }

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder, userService: UserService, logService: EventLogService) {

        if (properties.getBoolean("archivist.security.hmac.enabled")) {
            auth.authenticationProvider(hmacAuthenticationProvider())
        }
        auth
                .authenticationProvider(authenticationProvider())
                .authenticationEventPublisher(authenticationEventPublisher(userService, logService))

        /**
         * If its a unit test we add our rubber stamp authenticator.
         */
        if (ArchivistConfiguration.unittest) {
            auth.authenticationProvider(UnitTestAuthenticationProvider())
        }
    }

    @Bean
    @Autowired
    fun authenticationEventPublisher(userService: UserService, logService: EventLogService): AuthenticationEventPublisher {

        return object : AuthenticationEventPublisher {

            private val logger = LoggerFactory.getLogger(FormSecurityConfig::class.java)

            override fun publishAuthenticationSuccess(
                    authentication: Authentication) {
                try {
                    val user = authentication.principal as UserAuthed
                    userService.incrementLoginCounter(user)
                    logService.logAsync(UserLogSpec()
                            .setAction(LogAction.Login)
                            .setUser(user))
                } catch (e: Exception) {
                    // If we throw here, the authentication fails, so if we can't log
                    // it then nobody can login.  Sorry L337 hackers
                    logger.warn("Failed to log user authentication", e)
                    throw SecurityException(e)
                }

            }

            override fun publishAuthenticationFailure(
                    exception: AuthenticationException,
                    authentication: Authentication) {
                logger.info("Failed to authenticate: {}", authentication)
                logService.logAsync(UserLogSpec()
                        .setAction(LogAction.LoginFailure)
                        .setMessage(authentication.principal.toString() + " failed to login, reason "
                                + exception.message))
            }
        }
    }

    @Bean
    fun authenticationProvider(): AuthenticationProvider {
        return ZorroaAuthenticationProvider()
    }

    /**
     * Handles python/java client authentication.
     *
     * @return
     */
    @Bean
    fun hmacAuthenticationProvider(): AuthenticationProvider {
        return HmacAuthenticationProvider(properties.getBoolean("archivist.security.hmac.trust"))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MultipleWebSecurityConfig::class.java)
    }
}


