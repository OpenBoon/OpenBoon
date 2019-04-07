package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Test
    fun testGetEsRestClient() {
        val client = indexRoutingService.getOrgRestClient()
        assertEquals("unittest", client.route.indexName)
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
    }
}