package com.zorroa.archivist.rest

import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@WebAppConfiguration
class ActuatorTests : MockMvcTest() {

    @Test
    fun testInfoEndpoint() {
        val session = admin()
        val rsp = mvc.perform(MockMvcRequestBuilders.get("/actuator/info")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.deserialize(rsp.response.contentAsString, Json.GENERIC_MAP)
        assertEquals("Zorroa Archivist Server", result["description"])
        assertTrue("build.version" in result.keys)
    }

    @Test
    fun testHealthEndpoint() {
        val session = admin()
        val rsp = mvc.perform(MockMvcRequestBuilders.get("/actuator/health")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.deserialize(rsp.response.contentAsString, Json.GENERIC_MAP)
        assertEquals("UP", result["status"])
    }

    @Test
    fun testMetrics() {
        val session = admin()
        val rsp = mvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val result = Json.deserialize(rsp.response.contentAsString, Json.GENERIC_MAP)
        assertTrue("names" in result.keys)
    }

    @Test
    fun testMetricsFail() {
        val session = user()
        val rsp = mvc.perform(MockMvcRequestBuilders.get("/actuator/metrics")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().is4xxClientError)
                .andReturn()

        println(rsp.response.contentAsString)
    }
}