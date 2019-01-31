package com.zorroa.archivist.elastic

import com.zorroa.archivist.service.IndexRoutingService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class ElasticHealthCheck @Autowired constructor(val indexRoutingService: IndexRoutingService) : HealthIndicator {

    override fun health(): Health {
        val healthy = check()
        return if (!healthy) {
            val client = indexRoutingService.getEsRestClient()
            Health.down().withDetail("Elasticsearch down or not ready", client.route).build()
        }
        else {
            Health.up().build()
        }
    }

    fun check() : Boolean {
        return indexRoutingService.getEsRestClient().isAvailable()
    }

}