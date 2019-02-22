package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.clients.ElasticMapping
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Autowired
    lateinit var elasticSearchConfiguration : ElasticSearchConfiguration

    @Before
    fun init() {
        elasticSearchConfiguration.indexName = "unittest"
    }

    @Test
    fun testGetEsRestClient() {
        val client = indexRoutingService.getEsRestClient(getOrgId())
        assertEquals(client.route.routingKey, getOrgId().toString())
        assertEquals("unittest", client.route.indexName)
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
    }

    @Test
    fun testGetEsRestClientByRoute() {
        val client = indexRoutingService.getEsRestClient(getOrgId())
        val client2 = indexRoutingService.getEsRestClient(client.route)
        assertEquals(client2.route.routingKey, getOrgId().toString())
        assertEquals("unittest", client2.route.indexName)
        assertTrue(client2.indexExists())
        assertTrue(client2.isAvailable())
    }

    @Test
    fun testGetIndexRoute() {
        val mapping = ElasticMapping("foo", 1, mapOf())
        val route = indexRoutingService.getIndexRoute(mapping)
        assertEquals(route.indexName, "unittest")
    }

    @Test
    fun testGetIndexName() {
        // check to ensure unittest override is working
        val mapping = ElasticMapping("foo", 1, mapOf())
        val name = indexRoutingService.getIndexName(mapping)
        assertEquals("unittest", name)
        assertEquals("foo_v1", mapping.indexName)
    }

    @Test
    fun testCreateIndex() {
        val mapping = ElasticMapping("foo", 1, mapOf())
        val route = indexRoutingService.getIndexRoute(mapping)
        val newRoute =  indexRoutingService.createIndex(route.clusterUrl, mapping)
        val client = indexRoutingService.getEsRestClient(newRoute)
        assertTrue(client.indexExists())
    }

    @Test
    fun testAutoNamedIndex() {
        // Test if we set the index name to auto, that the index name
        // is generated from the mapping name.
        val orignalName = elasticSearchConfiguration.indexName
        elasticSearchConfiguration.indexName = "auto"
        try {
            val mapping = ElasticMapping("foo", 1, mapOf())
            val name = indexRoutingService.getIndexName(mapping)
            assertEquals("foo_v1", name)
        }
        finally {
            elasticSearchConfiguration.indexName = orignalName
        }
    }
}