package com.zorroa.archivist.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import java.net.URI

@Configuration
@ConfigurationProperties("server")
class ServerConfiguration {


    var fqdn: String? = null
    var port: Int? = 8066

    @Value("\${server.ssl.enabled}")
    var ssl : Boolean = false

    fun getUri() : String {
        val scheme = if (ssl) {
            "https"
        }
        else {
            "http"
        }
        return "$scheme://$fqdn:port"
    }

}

@Component
class NetworkEnvironment {

    @Autowired
    lateinit var serverProperties : ServerConfiguration

    @Autowired
    lateinit var properties: ApplicationProperties

    fun publicUri() : URI {
        return URI.create(serverProperties.getUri())
    }
}
