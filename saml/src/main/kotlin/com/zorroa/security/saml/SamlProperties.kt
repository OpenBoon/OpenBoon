package com.zorroa.security.saml

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * @property keystore the SSL keystore information
 * @property baseUrl The base URL the IDP should use to talk to this server.
 * @property landingPage The URL of the landing page once auth is successful.
 * @property logout Enable logout button for SAML users.
 * @property forceAuthN The identity provider MUST authenticate the presenter directly if the cookie times out.
 * @property maxAuthenticationAge The time before the server will attempt to reauth with the IDP
 */
@Configuration
@ConfigurationProperties("archivist.security.saml")
class SamlProperties {

    lateinit var keystore: MutableMap<String, String>
    lateinit var baseUrl: String
    lateinit var landingPage: String
    var logout = true
    var forceAuthN = false
    var maxAuthenticationAge : Long = 7200
}
