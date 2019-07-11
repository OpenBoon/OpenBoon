package com.zorroa.ldap

import com.zorroa.archivist.sdk.security.LdapUserDetailsPlugin
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.ldap.authentication.BindAuthenticator
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch

@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
open class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var properties: LdapProperties

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.formLogin().disable()
    }

    @Autowired
    open fun configureGlobal(auth: AuthenticationManagerBuilder) {
        if (!properties.enabled) {
            return
        }
        logger.info("Adding LDAP authentication provider")
        auth.authenticationProvider(ldapAuthenticationProvider())
    }

    @Bean
    @Throws(Exception::class)
    open fun ldapAuthenticationProvider(): AuthenticationProvider {
        val userDetailsService = ldapUserDetailsService()
        val contextSource = DefaultSpringSecurityContextSource(properties.url)
        contextSource.setBase(properties.base)
        contextSource.afterPropertiesSet()
        val ldapUserSearch = FilterBasedLdapUserSearch("", properties.filter, contextSource)
        val bindAuthenticator = BindAuthenticator(contextSource)
        bindAuthenticator.setUserSearch(ldapUserSearch)
        val ldapAuthenticationProvider = LdapAuthenticationProvider(bindAuthenticator, userDetailsService)
        ldapAuthenticationProvider.setUserDetailsContextMapper(userDetailsService)
        return ldapAuthenticationProvider
    }

    /**
     * If no LdapUserDetailsPlugin plugin is defined, use this one
     * as a default.
     **/
    @Bean
    @ConditionalOnMissingBean
    open fun ldapUserDetailsPlugin(): LdapUserDetailsPlugin {
        return StandardLdapUserDetailsPlugin()
    }

    @Bean
    open fun ldapUserDetailsService(): LdapUserDetailsServiceImpl {
        return LdapUserDetailsServiceImpl()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSecurityConfig::class.java)
    }
}
