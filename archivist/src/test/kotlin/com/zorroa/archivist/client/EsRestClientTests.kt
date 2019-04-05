package com.zorroa.archivist.client

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.getOrgId
import org.junit.Test
import kotlin.test.assertTrue

class EsRestClientTests : AbstractTest() {

    @Test
    fun testIsAvailable() {

        val client1 = indexRoutingService.getEsRestClient()
        assertTrue(client1.isAvailable())
    }

    @Test
    fun testIndexExists() {
        val client1 = indexRoutingService.getEsRestClient()
        assertTrue(client1.indexExists())
    }
}