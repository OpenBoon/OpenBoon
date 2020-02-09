package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssetSearchServiceTests : AbstractTest() {

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Before
    fun setUp() {
        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        spec.attrs = mapOf("analysis.zmlp.similarity.vector" to "AABBCC00")

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        assetService.batchCreate(batchCreate)
    }

    @Test
    fun testSearch() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search)
        assertEquals(1, rsp.hits.hits.size)
    }

    @Test
    fun testCount() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val count = assetSearchService.count(search)
        assertEquals(1, count)
    }

    @Test
    fun testScrollSearch() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search, mapOf("scroll" to arrayOf("1m")))
        assertEquals(1, rsp.hits.hits.size)
        assertNotNull(rsp.scrollId)
    }

    @Test
    fun testScroll() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search, mapOf("scroll" to arrayOf("5s")))
        assertNotNull(rsp.scrollId)

        val scroll = assetSearchService.scroll(mapOf(
            "scroll_id" to rsp.scrollId, "scroll" to "5s"))

        assertNotNull(scroll.scrollId)
        assertEquals(0, scroll.hits.hits.size)
    }

    @Test
    fun testClearScroll() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search, mapOf("scroll" to arrayOf("1m")))
        val result = assetSearchService.clearScroll(mapOf("scroll_id" to rsp.scrollId))
        assertTrue(result.isSucceeded)
    }

    @Test
    fun testSimilaritySearch() {
        val search =
            mapOf(
                "query" to mapOf(
                    "similarity" to mapOf(
                        "analysis.zmlp.similarity.vector" to listOf(mapOf("hash" to "AABBDD11"))
                    )
                )
            )

        val rsp = assetSearchService.search(search)
        assertEquals(1, rsp.hits.hits.size)
    }
}
