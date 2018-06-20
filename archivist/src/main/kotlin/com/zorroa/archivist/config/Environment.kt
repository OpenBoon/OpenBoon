package com.zorroa.archivist.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI

@Component
class NetworkEnvironment {

    @Autowired
    lateinit var properties: ApplicationProperties

    fun publicUri() : URI {
        return URI.create(properties.getString("server.fqdn"))
    }
}
