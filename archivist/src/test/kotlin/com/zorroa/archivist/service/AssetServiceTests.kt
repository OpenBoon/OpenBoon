package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun testBatchUpsert() {
        val assets = getTestAssets("set04/standard")
        val rsp = assetService.batchCreateOrReplace(BatchCreateAssetsRequest(assets))
        assertEquals(2, rsp.replaced)
        assertEquals(0, rsp.created)
    }

    @Test
    fun testDelete() {
        val page = searchService.search(Pager.first(), AssetSearch())
        assertTrue(assetService.delete(page[0].id))
        refreshIndex()

        val leftOvers = searchService.search(Pager.first(), AssetSearch())
        assertEquals(page.size() - leftOvers.size(), 1)
    }

    @Test
    fun batchDelete() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val rsp = assetService.batchDelete(ids)
        assertEquals(0, rsp.failures.size)
        assertEquals(2, rsp.totalDeleted)
        assertEquals(2, rsp.totalRequested)
    }
}