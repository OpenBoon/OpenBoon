package com.zorroa.ldap2

import com.zorroa.archivist.sdk.config.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.encoding.LdapShaPasswordEncoder
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.ldap.DefaultSpringSecurityContextSource
import org.springframework.security.ldap.authentication.BindAuthenticator
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch
import org.springframework.security.ldap.search.LdapUserSearch
import java.util.*


@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
open class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    @Autowired
    lateinit var properties : ApplicationProperties

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.formLogin().disable()
    }

    @Autowired
    open fun configureGlobal(auth: AuthenticationManagerBuilder) {
        if (!properties.getBoolean("archivist.security.ldap.enabled")) {
            return
        }
        logger.info("Adding LDAP authentication provider");
        auth.authenticationProvider(ldapAuthenticationProvider())
    }

    @Bean
    @Throws(Exception::class)
    open fun ldapAuthenticationProvider(): AuthenticationProvider {
        val url = properties.getString("archivist.security.ldap.url")
        val base = properties.getString("archivist.security.ldap.base")
        val filter = properties.getString("archivist.security.ldap.filter")

        val userDetailsService = ldapUserDetailsService()
        val contextSource = DefaultSpringSecurityContextSource(url)
        contextSource.setBase(base)
        contextSource.afterPropertiesSet()
        val ldapUserSearch = FilterBasedLdapUserSearch("", filter, contextSource)
        val bindAuthenticator = BindAuthenticator(contextSource)
        bindAuthenticator.setUserSearch(ldapUserSearch)
        val ldapAuthenticationProvider = LdapAuthenticationProvider(bindAuthenticator, userDetailsService)
        ldapAuthenticationProvider.setUserDetailsContextMapper(userDetailsService)
        return ldapAuthenticationProvider
    }

    @Bean
    open fun ldapUserDetailsService() : LdapUserDetailsServiceImpl  {
        return LdapUserDetailsServiceImpl()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WebSecurityConfig::class.java)
    }
}

