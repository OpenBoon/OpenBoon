package com.zorroa.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource


@ConfigurationProperties("security")
class SecurityProperties {

    var jwt: JwtProperties? = null

    class JwtProperties {

        var keyStore: Resource? = null
        var keyStorePassword: String? = null
        var keyPairAlias: String? = null
        var keyPairPassword: String? = null
    }
}