package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.BatchUpdateClipProxyRequest
import boonai.archivist.domain.UpdateClipProxyRequest
import boonai.archivist.domain.ClipSpec
import boonai.archivist.domain.TimelineClipSpec
import boonai.archivist.domain.CreateTimelineResponse
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.TimelineSpec
import boonai.archivist.domain.TrackSpec
import boonai.archivist.domain.WebVTTFilter
import boonai.archivist.util.bd
import boonai.common.util.Json
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.join.query.JoinQueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            TimelineClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            TimelineClipSpec(BigDecimal(11.2), BigDecimal(12.5), listOf("cat"), 0.5),
            TimelineClipSpec(BigDecimal(11.684), BigDecimal(14.231), listOf("cat"), 0.2)
        )
        val track = TrackSpec("cats", clips)
        timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track), false)
        rsp = clipService.createClips(timeline)
        refreshElastic()
    }

    @Test
    fun createClip() {
        val parent = getSample(1, "video")[0]
        val clipSpec = ClipSpec(parent.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)

        assertEquals(parent.id, clip.assetId)
        assertEquals("test", clip.timeline)
        assertEquals("test", clip.track)
        assertEquals(1.0.bd(), clip.start)
        assertEquals(5.0.bd(), clip.stop)
    }

    @Test
    fun testGetClip() {
        val parent = getSample(1, "video")[0]
        val clipSpec = ClipSpec(parent.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip1 = clipService.createClip(clipSpec)
        refreshElastic()
        val clip2 = clipService.getClip(clip1.id)
        assertEquals(clip1.id, clip2.id)
        assertEquals(clip1.timeline, clip2.timeline)
        assertEquals(clip1.track, clip2.track)
    }

    @Test
    fun deleteClip() {
        val parent = getSample(1, "video")[0]
        val clipSpec = ClipSpec(parent.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)
        assertTrue(clipService.deleteClip(clip.id))
        assertFalse(clipService.deleteClip(clip.id))
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
            TimelineClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.9),
            // All these should be skipped.
            TimelineClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.1),
            TimelineClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.4),
            TimelineClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.6),
            TimelineClipSpec(BigDecimal.ONE, BigDecimal(12.534), listOf("dog"), 0.11)
        )
        val track = TrackSpec("dogs", clips)
        val timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track), false)
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
    fun testGetWebVttByWebVTTFilter() {
        val output = ByteArrayOutputStream()
        val filter = WebVTTFilter(asset.id)
        clipService.streamWebvtt(filter, output)
        val webvtt = String(output.toByteArray())

        // Check the times.
        assertTrue("00:00:11.684 --> 00:00:14.231" in webvtt)
        assertTrue("00:00:11.200 --> 00:00:12.500" in webvtt)
        assertTrue("00:00:11.684 --> 00:00:14.231" in webvtt)
    }

    @Test
    fun testGetWebVttBySearch() {
        val output = ByteArrayOutputStream()
        clipService.streamWebvtt(asset, mapOf(), output)

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

    @Test
    fun testSetProxy() {
        val file = FileStorage("abc123", "proxy.jpg", "proxy", "image/jpeg", 100, mapOf())
        val proxy = UpdateClipProxyRequest(listOf(file), "abc123")

        val clipSpec = ClipSpec(asset.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)

        refreshElastic()
        assertTrue(clipService.setProxy(clip.id, proxy))
        refreshElastic()

        val uclip = clipService.getClip(clip.id)
        assertEquals(1, uclip.files?.size)
        assertEquals("abc123", uclip.simhash)
    }

    @Test
    fun testBatchSetProxy() {
        val file = FileStorage("abc123", "proxy.jpg", "proxy", "image/jpeg", 100, mapOf())
        val proxy = UpdateClipProxyRequest(listOf(file), "abc123")

        val clipSpec1 = ClipSpec(asset.id, "test1", "test1", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip1 = clipService.createClip(clipSpec1)

        val clipSpec2 = ClipSpec(asset.id, "test2", "test2", 5.0.bd(), 10.0.bd(), listOf("dog"))
        val clip2 = clipService.createClip(clipSpec2)

        val req = BatchUpdateClipProxyRequest(
            asset.id,
            mapOf(clip1.id to proxy, clip2.id to proxy)
        )

        val rsp = clipService.batchSetProxy(req)
        assertTrue(rsp.success)

        refreshElastic()

        val uclip1 = clipService.getClip(clip1.id)
        assertEquals(1, uclip1.files?.size)
        assertEquals("abc123", uclip1.simhash)

        val uclip2 = clipService.getClip(clip1.id)
        assertEquals(1, uclip2.files?.size)
        assertEquals("abc123", uclip2.simhash)
    }

    @Test
    fun testGetCollapseKeys() {
        val time = BigDecimal(50.5).setScale(3, RoundingMode.HALF_UP)
        val keys = clipService.getCollapseKeys("abc123", time)
        assertEquals("abc123_50_500", keys["startTime"])
        assertEquals("abc123_50", keys["1secWindow"])
        assertEquals("abc123_10", keys["5secWindow"])
        assertEquals("abc123_5", keys["10secWindow"])
    }
}
