package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    @Autowired
    lateinit var assetService: AssetService

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
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
        assertEquals(0, rsp.errors.size)
        assertEquals(2, rsp.deletedAssetIds.size)
        assertEquals(2, rsp.totalRequested)
    }
}