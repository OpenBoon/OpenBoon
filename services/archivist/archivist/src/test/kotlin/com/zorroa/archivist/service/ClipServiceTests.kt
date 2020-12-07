package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.ClipSpec
import com.zorroa.archivist.domain.CreateTimelineResponse
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.domain.TrackSpec
import com.zorroa.zmlp.util.Json
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.join.query.JoinQueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClipServiceTests : AbstractTest() {

    @Autowired
    lateinit var clipService: ClipService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    lateinit var asset: Asset
    lateinit var timeline: TimelineSpec
    lateinit var rsp: CreateTimelineResponse

    @Before
    fun createTestData() {
        addTestAssets("video")

        asset = getSample(1, "video")[0]
        val clips = listOf(
            ClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            ClipSpec(BigDecimal(11.2), BigDecimal(12.5), listOf("cat"), 0.5),
            ClipSpec(BigDecimal(11.684), BigDecimal(14.231), listOf("cat"), 0.2)
        )
        val track = TrackSpec("cats", clips)
        timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track))
        rsp = clipService.createClips(timeline)
        refreshElastic()
    }

    @Test
    fun createClipsFromTimeline() {
        assertEquals(asset.id, rsp.assetId)
        assertEquals(3, rsp.created)
        assertTrue(rsp.failed.isEmpty())
    }

    @Test
    fun createDuplicateClips() {
        val asset = getSample(2, "video")[1]
        val clips = listOf(
            ClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.9),
            // All these should be skipped.
            ClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.1),
            ClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.4),
            ClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.6),
            ClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.11)
        )
        val track = TrackSpec("dogs", clips)
        val timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track))
        val rsp = clipService.createClips(timeline)
        refreshElastic()
        assertEquals(1, rsp.created)

        val search = clipService.searchClips(asset, mapOf(), mapOf())
        val doc = Asset("test", search.hits.hits[0].sourceAsMap)
        assertEquals(0.9, doc.getAttr("clip.score"))
    }

    @Test
    fun testSearchTimeline() {
        val search = clipService.searchClips(asset, mapOf(), mapOf())
        assertEquals(3, search.hits.hits.size)
    }

    @Test
    fun testAssetParentChildSearch() {
        val ssb = SearchSourceBuilder()
        ssb.query(
            JoinQueryBuilders.hasChildQuery(
                "clip", QueryBuilders.termQuery("clip.track", "cats"), ScoreMode.None
            )
        )
        val result = assetSearchService.search(ssb, mapOf())
        assertEquals(1, result.hits.hits.size)
    }

    @Test
    fun testGetWebVttBySearch() {
        val output = ByteArrayOutputStream()
        clipService.getWebvtt(asset, mapOf(), output)

        val webvtt = String(output.toByteArray())

        // Check the times.
        assertTrue("00:00:11.684 --> 00:00:14.231" in webvtt)
        assertTrue("00:00:11.200 --> 00:00:12.500" in webvtt)
        assertTrue("00:00:11.684 --> 00:00:14.231" in webvtt)

        val startPos = webvtt.indexOf('{')
        val endPos = webvtt.indexOf('}')
        val json = webvtt.substring(startPos, endPos + 1)

        val data = Json.Mapper.readValue(json, Json.GENERIC_MAP)
        assertEquals("zvi-label-detection", data["timeline"])
        assertEquals("cats", data["track"])
        assertEquals(listOf("cat"), data["content"])
        assertEquals(0.5, data["score"])
    }

    @Test
    fun testDeleteClipsByAssets() {
        clipService.deleteClips(listOf(asset.id))
        refreshElastic()

        val search = clipService.searchClips(asset, mapOf(), mapOf())
        assertEquals(0, search.hits.hits.size)
    }
}
