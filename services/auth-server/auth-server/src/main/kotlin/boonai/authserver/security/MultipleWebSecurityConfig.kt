package boonai.authserver.security

import boonai.authserver.domain.ValidationKey
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@ConfigurationProperties("boonai.security")
class SecurityProperties {
    var inceptionKey: String? = null
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
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    @EnableGlobalMethodSecurity(securedEnabled = true)
    class ActuatorSecurityConfig : WebSecurityConfigurerAdapter() {

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/monitor/**")
                .httpBasic()
                .and()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
                .hasAuthority("MONITOR")
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
        }
    }

    @Configuration
    @EnableGlobalMethodSecurity(securedEnabled = true)
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
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

    @Value("\${management.endpoints.password}")
    lateinit var monitorPassword: String

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(jwtAuthenticationProvider)
            .inMemoryAuthentication()
            .withUser("monitor")
            .password(passwordEncoder().encode(monitorPassword))
            .authorities("MONITOR")
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun serviceKey(): ValidationKey {
        return loadServiceKey(securityProperties.inceptionKey)
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
