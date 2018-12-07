package com.zorroa.archivist.client

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.clients.IndexRoute
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EsRestClientTests : AbstractTest() {

    @Test
    fun testIsAvailable() {
        val client1 = indexRoutingService[getOrgId()]
        assertTrue(client1.isAvailable())

        val client2 = indexRoutingService.getEsRestClient(
                IndexRoute("http://locahost:9999", "foo", "bar"))
        assertFalse(client2.isAvailable())
    }

    @Test
    fun testIndexExists() {
        val client1 = indexRoutingService[getOrgId()]
        assertTrue(client1.indexExists())

        val client2 = indexRoutingService.getEsRestClient(
                IndexRoute(client1.route.clusterUrl, "foo", "bar"))
        assertFalse(client2.indexExists())
    }
}