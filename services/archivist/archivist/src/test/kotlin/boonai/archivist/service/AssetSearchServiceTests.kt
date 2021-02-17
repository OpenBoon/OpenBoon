package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.common.util.Json
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
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
        val labels = listOf(
            mapOf("label" to "toucan", "score" to 0.5)
        )

        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        val spec2 = AssetSpec("https://i.imgur.com/abc123442.jpg")

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(spec, spec2)
        )
        val rsp = assetService.batchCreate(batchCreate)
        val id = rsp.created[0]
        val asset = assetService.getAsset(id)
        asset.setAttr("analysis.boonai-image-similarity.simhash", "AABBCC00")
        asset.setAttr("analysis.boonai-multi-similarity.simhash", listOf("AABBCC00", "CCCCCCCC", "AABBDDDD"))
        asset.setAttr("analysis.boonai-label-detection.predictions", labels)
        assetService.index(id, asset.document, true)

        indexRoutingService.getProjectRestClient().refresh()
    }

    @Test
    fun testSearchSourceBuilderToMap() {
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.termsQuery("bob", listOf("abc")))

        val map = assetSearchService.searchSourceBuilderToMap(ssb)
        val json = Json.Mapper.writeValueAsString(map)
        val expected =
            """{"query":{"terms":{"bob":["abc"],"boost":1.0}}}"""
        assertEquals(expected, json)
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
    fun testSearchWithSearchSourceBuilder() {
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.matchAllQuery())

        val rsp = assetSearchService.search(ssb, mapOf())
        assertEquals(2, rsp.hits.hits.size)
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
    fun testCountWithSearchSourceBuilder() {
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.matchAllQuery())

        val count = assetSearchService.count(ssb)
        assertEquals(2, count)
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

        val scroll = assetSearchService.scroll(
            mapOf(
                "scroll_id" to rsp.scrollId, "scroll" to "5s"
            )
        )

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
        val query =
            """
            {
              "query": {
                "function_score": {
                  "functions": [
                    {
                      "script_score": {
                        "script": {
                          "source": "similarity",
                          "lang": "zorroa-similarity",
                          "params": {
                            "minScore": 0.50,
                            "field": "analysis.boonai-image-similarity.simhash",
                            "hashes": [
                              "AABBDD00"
                            ]
                          }
                        }
                      }
                    }
                  ],
                  "score_mode": "multiply",
                  "boost_mode": "replace",
                  "max_boost": 3.4028235E38,
                  "min_score": 0.50,
                  "boost": 1.0
                }
              }
            }
        """

        Json.prettyPrint(indexRoutingService.getProjectRestClient().getMapping())

        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(1, rsp.hits.hits.size)
        assertTrue(rsp.hits.hits[0].score > 0.98)
    }

    @Test
    fun testSimilaritySearchOnArray() {
        val query =
            """
            {
              "query": {
                "script_score": {
                  "query": {
                    "match_all": {}
                  },
                  "script": {
                    "source": "similarity",
                    "lang": "zorroa-similarity",
                    "params": {
                      "minScore": 1.0,
                      "field": "analysis.boonai-multi-similarity.simhash",
                      "hashes": [
                        "CCCCCCCC"
                      ]
                    }
                  },
                  "boost": 1.0,
                  "min_score": 1.0
                }
              }
            }
            """

        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(1, rsp.hits.hits.size)
        assertEquals(rsp.hits.hits[0].score, 1.0f)
    }

    @Test
    fun testSimilaritySearchOnEmptyField() {
        val query =
            """
            {
              "query": {
                "script_score": {
                  "query": {
                    "match_all": {}
                  },
                  "script": {
                    "source": "similarity",
                    "lang": "zorroa-similarity",
                    "params": {
                      "minScore": 1.0,
                      "field": "analysis.boonai-foo-similarity.simhash",
                      "hashes": [
                        "CCCCCCCC"
                      ]
                    }
                  },
                  "boost": 1.0,
                  "min_score": 1.0
                }
              }
            }
            """

        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(0, rsp.hits.hits.size)
    }

    @Test
    fun testSimilaritySearch_noHits() {
        val query =
            """
            {
              "query": {
                "function_score": {
                  "functions": [
                    {
                      "script_score": {
                        "script": {
                          "source": "similarity",
                          "lang": "zorroa-similarity",
                          "params": {
                            "minScore": 0.50,
                            "field": "analysis.boonai-image-similarity.simhash",
                            "hashes": [
                              "PPPPPPPP"
                            ]
                          }
                        }
                      }
                    }
                  ],
                  "score_mode": "multiply",
                  "boost_mode": "replace",
                  "max_boost": 3.4028235E38,
                  "min_score": 0.50,
                  "boost": 1.0
                }
              }
            }
            """
        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(0, rsp.hits.hits.size)
    }
}
