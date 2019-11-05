package com.zorroa.archivist.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.core.io.Resource


@ConfigurationProperties("security")
class SecurityProperties {

    var jwt: JwtProperties? = null

    class JwtProperties {

        var publicKey: Resource? = null
    }

}