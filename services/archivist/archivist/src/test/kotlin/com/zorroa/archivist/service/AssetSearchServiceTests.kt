package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.zmlp.util.Json
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
        val spec2 = AssetSpec("https://i.imgur.com/abc123442.jpg")

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(spec, spec2)
        )
        val rsp = assetService.batchCreate(batchCreate)
        val id = rsp.created[0]
        val asset = assetService.getAsset(id)
        asset.setAttr("analysis.zmlp.similarity.vector", "AABBCC00")
        assetService.index(id, asset.document)

        indexRoutingService.getProjectRestClient().refresh()
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
        val query = """{
            "query": {
                "function_score" : {
                    "query" : {
                      "match_all" : {
                        "boost" : 1.0
                      }
                    },
                    "functions" : [
                      {
                        "filter" : {
                          "match_all" : {
                            "boost" : 1.0
                          }
                        },
                        "script_score" : {
                          "script" : {
                            "source" : "similarity",
                            "lang" : "zorroa-similarity",
                            "params" : {
                              "minScore" : 0.50,
                              "field" : "analysis.zmlp.similarity.vector",
                              "hashes" : ["AABBDD00"],
                              "weights" : [1.0]
                            }
                          }
                        }
                      }
                    ],
                    "score_mode" : "multiply",
                    "boost_mode" : "replace",
                    "max_boost" : 3.4028235E38,
                    "min_score" : 0.50,
                    "boost" : 1.0
                  }
                }
            }
        """.trimIndent()
        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(1, rsp.hits.hits.size)
    }
}
