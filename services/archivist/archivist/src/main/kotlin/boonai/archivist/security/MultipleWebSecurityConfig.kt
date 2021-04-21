package boonai.archivist.security

import boonai.archivist.config.ApplicationProperties
import boonai.archivist.service.ProjectService
import boonai.common.apikey.AuthServerClient
import boonai.common.apikey.AuthServerClientImpl
import boonai.common.apikey.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

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
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Autowired
        lateinit var apiKeyAuthorizationFilter: ApiKeyAuthorizationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/api/**")
                .addFilterAfter(
                    apiKeyAuthorizationFilter,
                    UsernamePasswordAuthenticationFilter::class.java
                )
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().authenticated()
                .and().csrf().disable()
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
                .addFilterBefore(
                    analystAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter::class.java
                )
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
        lateinit var apiKeyAuthorizationFilter: ApiKeyAuthorizationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/monitor/**")
                .httpBasic()
                .and()
                .csrf().disable()
                .addFilterBefore(
                    apiKeyAuthorizationFilter,
                    UsernamePasswordAuthenticationFilter::class.java
                )
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
                .hasAnyAuthority(Permission.SystemMonitor.name)
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 4)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class RootSecurityConfig : WebSecurityConfigurerAdapter() {

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/**")
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/error").permitAll()
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
            .authenticationProvider(apiKeyAuthenticationProvider())
            .inMemoryAuthentication()
            .withUser("monitor").password(passwordEncoder().encode(monitorPassword))
            .authorities(Permission.SystemMonitor.name)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * An AuthenticationProvider that handles previously validated JWT claims.
     */
    @Bean
    fun apiKeyAuthenticationProvider(): ApiKeyAuthenticationProvider {
        return ApiKeyAuthenticationProvider()
    }

    @Bean
    fun apiKeyAuthenticationFilter(projectService: ProjectService): ApiKeyAuthorizationFilter {
        return ApiKeyAuthorizationFilter(authServerClient(), projectService)
    }

    @Bean
    fun authServerClient(): AuthServerClient {
        val client = AuthServerClientImpl(
            properties.getString("boonai.security.auth-server.url"),
            properties.getString("boonai.security.auth-server.service-key")
        )
        logger.info("Loaded inception key: {}", client.serviceKey?.accessKey?.substring(8))
        return client
    }

    @Bean
    @Autowired
    fun apiKeyFilterRegistration(projectService: ProjectService): FilterRegistrationBean<ApiKeyAuthorizationFilter> {
        val bean = FilterRegistrationBean<ApiKeyAuthorizationFilter>()
        bean.filter = apiKeyAuthenticationFilter(projectService)
        bean.isEnabled = false
        return bean
    }

    @Bean
    @Autowired
    fun analystFilterRegistration(analystAuthenticationFilter: AnalystAuthenticationFilter):
        FilterRegistrationBean<AnalystAuthenticationFilter> {
            val bean = FilterRegistrationBean<AnalystAuthenticationFilter>()
            bean.filter = analystAuthenticationFilter
            bean.isEnabled = false
            return bean
        }

    companion object {
        private val logger = LoggerFactory.getLogger(MultipleWebSecurityConfig::class.java)
    }
}
