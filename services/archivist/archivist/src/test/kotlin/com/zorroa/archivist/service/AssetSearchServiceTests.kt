package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AssetSearchServiceTests : AbstractTest() {

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Test
    fun testSearch() {
        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("https://i.imgur.com/LRoLTlK.jpg"))
        )
        assetService.batchCreate(batchCreate)

        // Note that here, scroll is not allowed yet the result should have no scroll id.
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg")),
            "scroll" to "2s"
        )

        val rsp = assetSearchService.search(search)
        assertEquals(1, rsp.hits.hits.size)
        assertNull(rsp.scrollId)
    }

    @Test
    fun testSimilaritySearch() {
        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        spec.attrs = mapOf("analysis.zmlp.similarity.vector" to "AABBCC00")

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(spec)
        )
        assetService.batchCreate(batchCreate)

        // Note that here, scroll is not allowed yet the result should have no scroll id.
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
