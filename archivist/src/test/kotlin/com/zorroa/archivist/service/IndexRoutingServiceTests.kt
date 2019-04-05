package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.getOrgId
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Test
    fun testGetEsRestClient() {
        val client = indexRoutingService.getEsRestClient()
        assertEquals("unittest", client.route.indexName)
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
    }
}