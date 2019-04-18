package com.zorroa.security.saml

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("archivist.security.saml")
class SamlProperties {

    lateinit var keystore: MutableMap<String, String>
    var discovery = true
    lateinit var baseUrl: String
    lateinit var landingPage: String
    var isLogout = true

}
