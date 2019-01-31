package com.zorroa.archivist.elastic

import com.zorroa.archivist.service.IndexRoutingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class ElasticHealthCheck @Autowired constructor(val indexRoutingService: IndexRoutingService) : HealthIndicator {

    override fun health(): Health {
        val client = indexRoutingService.getEsRestClient()
        return if (check()) {
            Health.up().build()
        }
        else {
            Health.down().withDetail("Elasticsearch down or not ready", client.route).build()
        }
    }

    fun check() : Boolean {
        return indexRoutingService.getEsRestClient().isAvailable()
    }

}