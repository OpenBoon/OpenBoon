package com.zorroa.irm.studio.repository

import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.irm.studio.AbstractTest
import com.zorroa.irm.studio.rest.EsRestClientCache
import com.zorroa.irm.studio.rest.IndexRouteClient
import org.elasticsearch.action.get.GetRequest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class IndexDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    @Autowired
    lateinit var restClient: EsRestClientCache

    @Autowired
    lateinit var indexRouteClient: IndexRouteClient

    @Test
    fun testIndexDocument() {
        val asset = Asset(UUID.randomUUID(),UUID.randomUUID(), mapOf())
        val doc = Document(asset.id.toString(), mapOf("foo" to "bar"))
        indexDao.indexDocument(asset, doc)
        Thread.sleep(2000)

        // Doesn't matter what ID we send here.
        var route = indexRouteClient.getIndexRoute(UUID.randomUUID())

        val indexed =
                restClient.getClient(route).client.get(GetRequest(route.indexName, "asset", doc.id))
        assertEquals(indexed.id, doc.id)
    }
}
