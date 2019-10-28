package com.zorroa.archivist.client

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsRestClientTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testIsAvailable() {
        val client1 = indexRoutingService.getOrgRestClient()
        assertTrue(client1.isAvailable())
    }

    @Test
    fun testIndexExists() {
        val client1 = indexRoutingService.getOrgRestClient()
        assertTrue(client1.indexExists())
    }

    @Test
    fun testUpdateAndGetMapping() {
        val client = indexRoutingService.getOrgRestClient()
        assertTrue(
            client.updateMapping(
                mapOf("properties" to mapOf("name" to mutableMapOf<String, Any>("type" to "keyword")))
            )
        )
        val mapping = client.getMapping()
        val doc = Document(mapping)
        assertEquals("keyword", doc.getAttr("unittest.mappings.asset.properties.name.type", String::class.java))
    }
}