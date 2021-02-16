package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.Asset
import boonai.archivist.domain.BatchUpdateClipProxyRequest
import boonai.archivist.domain.ClipSpec
import boonai.archivist.domain.TimelineClipSpec
import boonai.archivist.domain.CreateTimelineResponse
import boonai.archivist.domain.FileStorage
import boonai.archivist.domain.TimelineSpec
import boonai.archivist.domain.TrackSpec
import boonai.archivist.domain.UpdateClipProxyRequest
import boonai.archivist.service.ClipService
import boonai.archivist.util.bd
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal

class ClipControllerTests : MockMvcTest() {

    lateinit var asset: Asset
    lateinit var timeline: TimelineSpec
    lateinit var rsp: CreateTimelineResponse

    @Autowired
    lateinit var clipService: ClipService

    @Before
    fun createTestData() {
        addTestAssets("video")

        asset = getSample(1)[0]
        val clips = listOf(
            TimelineClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            TimelineClipSpec(BigDecimal("11.2"), BigDecimal("12.5"), listOf("cat"), 0.5)
        )
        val track = TrackSpec("cats", clips)
        timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track), false)
    }

    @Test
    fun testCreateWithTimeline() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/clips/_timeline")
                .headers(admin())
                .content(Json.serialize(timeline))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.created", CoreMatchers.equalTo(2)))
            .andReturn()
    }

    @Test
    fun testCreateClip() {
        val clipSpec = ClipSpec(asset.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/clips")
                .headers(admin())
                .content(Json.serialize(clipSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.id",
                    CoreMatchers.equalTo("AuIY7Pm7NJGZuhYjH8nYfB_fDFUe59ip")
                )
            )
            .andReturn()
    }

    @Test
    fun testGet() {
        val clipSpec = ClipSpec(asset.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)
        refreshElastic()

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/clips/${clip.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(clip.id)))
            .andReturn()
    }

    @Test
    fun testDeleteClip() {
        val clipSpec = ClipSpec(asset.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/clips/${clip.id}")
                .headers(admin())
                .content(Json.serialize(clipSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }

    @Test
    fun testUpdateClipPrpoxy() {
        val clipSpec = ClipSpec(asset.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)
        refreshElastic()

        val req = UpdateClipProxyRequest(
            listOf(
                FileStorage("12345", "foo", "foo", "image/jpeg", 1000L, mapOf())
            ),
            "ABC123"
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/clips/${clip.id}/_proxy")
                .headers(admin())
                .content(Json.serialize(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }

    @Test
    fun testBatchUpdateClipPrpoxy() {
        val clipSpec = ClipSpec(asset.id, "test", "test", 1.0.bd(), 5.0.bd(), listOf("cat"))
        val clip = clipService.createClip(clipSpec)
        refreshElastic()

        val update = UpdateClipProxyRequest(
            listOf(
                FileStorage("12345", "foo", "foo", "image/jpeg", 1000L, mapOf())
            ),
            "ABC123"
        )

        val req = BatchUpdateClipProxyRequest(asset.id, mapOf(clip.id to update))

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/clips/_batch_update_proxy")
                .headers(admin())
                .content(Json.serialize(req))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.success",
                    CoreMatchers.equalTo(true)
                )
            )
            .andReturn()
    }
}
