package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.ClipSpec
import com.zorroa.archivist.domain.CreateTimelineResponse
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.domain.TrackSpec
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal

class ClipControllerTests : MockMvcTest() {

    lateinit var asset: Asset
    lateinit var timeline: TimelineSpec
    lateinit var rsp: CreateTimelineResponse

    @Before
    fun createTestData() {
        addTestAssets("video")

        asset = getSample(1)[0]
        val clips = listOf(
            ClipSpec(BigDecimal.ONE, BigDecimal.TEN, listOf("cat"), 0.5),
            ClipSpec(BigDecimal("11.2"), BigDecimal("12.5"), listOf("cat"), 0.5)
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
}
