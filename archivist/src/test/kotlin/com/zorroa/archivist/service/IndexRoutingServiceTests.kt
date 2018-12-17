package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.clients.ElasticMapping
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Test
    fun testGetEsRestClient() {
        val client = indexRoutingService.getEsRestClient(getOrgId())
        assertEquals(client.route.routingKey, getOrgId().toString())
        assertTrue(client.route.indexName.startsWith("assets_"))
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
    }

    @Test
    fun testGetEsRestClientByRoute() {
        val client = indexRoutingService.getEsRestClient(getOrgId())
        val client2 = indexRoutingService.getEsRestClient(client.route)
        assertEquals(client2.route.routingKey, getOrgId().toString())
        assertTrue(client2.route.indexName.startsWith("assets_"))
        assertTrue(client2.indexExists())
        assertTrue(client2.isAvailable())
    }

    @Test
    fun testGetIndexRoute() {
        val mapping = ElasticMapping("foo", 1, mapOf())
        val route = indexRoutingService.getIndexRoute(mapping)
        assertEquals(route.indexName, "foo_v1")
    }

    @Test
    fun testCreateIndex() {
        val mapping = ElasticMapping("foo", 1, mapOf())
        val route = indexRoutingService.getIndexRoute(mapping)
        val newRoute =  indexRoutingService.createIndex(route.clusterUrl, mapping)
        val client = indexRoutingService.getEsRestClient(newRoute)
        assertTrue(client.indexExists())
    }
}