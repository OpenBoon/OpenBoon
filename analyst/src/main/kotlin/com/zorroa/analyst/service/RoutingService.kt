package com.zorroa.analyst.service

import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.domain.IndexRoute
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.util.*
import javax.annotation.PostConstruct


@Configuration
@ConfigurationProperties("analyst.routing")
class RoutingProperties {
    var defaultClusterUrl : String? = null
}

/**
 * The ESClientCache uses a routing service to figure out what ES
 * cluster/index a client should be using.
 */
class IndexRoutingServiceImpl : IndexRoutingService {

    @Autowired
    lateinit var routingProperties: RoutingProperties

    @PostConstruct
    fun init() {
        logger.info("Default ES cluster {}", routingProperties.defaultClusterUrl)
    }

    override fun getIndexRoute(orgId: UUID): IndexRoute {
        return IndexRoute(UUID.fromString("00000000-9998-8888-7777-666666666666"),
                "100",
                routingProperties.defaultClusterUrl!!,
                "zorroa_v10",
                null)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(K8SchedulerServiceImpl::class.java)
    }
}

