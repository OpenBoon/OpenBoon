package boonai.archivist.client

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsRestClientTests : AbstractTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testIsAvailable() {
        val client1 = indexRoutingService.getProjectRestClient()
        assertTrue(client1.isAvailable())
    }

    @Test
    fun testIndexExists() {
        val client1 = indexRoutingService.getProjectRestClient()
        assertTrue(client1.indexExists())
    }

    @Test
    fun testUpdateAndGetMapping() {
        val client = indexRoutingService.getProjectRestClient()
        assertTrue(
            client.updateMapping(
                mapOf("properties" to mapOf("name" to mutableMapOf<String, Any>("type" to "keyword")))
            )
        )
        val mapping = client.getMapping()
        val doc = Asset(mapping.toMutableMap())
        val index = client.route.indexName
        assertEquals("keyword", doc.getAttr("$index.mappings.properties.name.type", String::class.java))
    }
}
