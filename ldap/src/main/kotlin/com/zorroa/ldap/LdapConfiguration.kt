package com.zorroa.ldap

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan("com.zorroa.ldap")
open class LdapConfiguration

@Configuration
@ConfigurationProperties("archivist.security.ldap")
open class LdapProperties {
    var enabled: Boolean = false
    var url: String? = null
    var base: String? = null
    var filter: String? = null
}
