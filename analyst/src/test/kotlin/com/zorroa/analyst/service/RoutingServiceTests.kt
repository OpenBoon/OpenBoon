package com.zorroa.analyst.service

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.clients.IndexRoutingService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class RoutingServiceTests : AbstractTest() {

    @Autowired
    lateinit var routingService: IndexRoutingService

    @Autowired
    lateinit var routingProperties: RoutingProperties

    /**
     * Test to ensure we're getting the hard coded default
     */
    @Test
    fun testGetIndexRoute() {
        val route = routingService.getIndexRoute(UUID.randomUUID())
        assertEquals(routingProperties.defaultClusterUrl, route.clusterUrl)
        assertEquals("zorroa_v10", route.indexName)
    }
}
