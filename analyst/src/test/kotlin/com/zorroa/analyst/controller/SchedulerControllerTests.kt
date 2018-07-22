package com.zorroa.analyst.controller

import com.zorroa.analyst.AbstractMvcTest
import com.zorroa.analyst.domain.UpdateStatus
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertNotNull

@WebAppConfiguration
class SchedulerControllerTests : AbstractMvcTest() {

    @Test
    fun testPause() {

        val req = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/scheduler/_pause")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val kpl = Json.deserialize(req.response.contentAsByteArray, UpdateStatus::class.java)
        assertNotNull(kpl.status["pause"])
    }

    @Test
    fun testResume() {
        val req = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/scheduler/_resume")
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        val kpl = Json.deserialize(req.response.contentAsByteArray, UpdateStatus::class.java)
        assertNotNull(kpl.status["resume"])
    }
}
