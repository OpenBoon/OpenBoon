package com.zorroa.analyst.repository

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.clients.EsClientCache
import com.zorroa.common.clients.IndexRoutingService
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import org.elasticsearch.action.get.GetRequest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class IndexDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexDao: IndexDao

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    lateinit var esClientCache : EsClientCache

    @Test
    fun testIndexDocument() {
        val asset = Asset(UUID.randomUUID(),UUID.randomUUID(), mapOf())
        val doc = Document(asset.id.toString(), mapOf("foo" to "bar"))
        indexDao.indexDocument(asset, doc)
        Thread.sleep(2000)

        // Doesn't matter what ID we send here.
        var route = indexRoutingService.getIndexRoute(UUID.randomUUID())

        val indexed =
                esClientCache.get(route).client.get(GetRequest(route.indexName, "asset", doc.id))
        assertEquals(indexed.id, doc.id)
    }
}
