package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.ClipSpec
import com.zorroa.archivist.domain.TimelineClipSpec
import com.zorroa.archivist.domain.CreateTimelineResponse
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.domain.TrackSpec
import com.zorroa.archivist.service.ClipService
import com.zorroa.archivist.util.bd
import com.zorroa.zmlp.util.Json
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
        timeline = TimelineSpec(asset.id, "zvi-label-detection", listOf(track))
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
}
