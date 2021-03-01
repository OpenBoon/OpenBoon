package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@WebAppConfiguration
class ActuatorTests : MockMvcTest() {

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun testInfoEndpoint() {
        val rsp = mvc.perform(
            MockMvcRequestBuilders.get("/monitor/info")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue(rsp.response.contentAsString, Json.GENERIC_MAP)
        assertEquals("Zorroa Archivist Server", result["description"])
        assertTrue("build.version" in result.keys)
    }

    @Test
    fun testHealthEndpoint() {
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/health")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andReturn()
    }

    @Test
    fun testMetrics() {
        // Need to auth with monitor username/pass
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/metrics")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.names").exists())
            .andReturn()
    }

    @Test
    fun testMetricsFail() {
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/metrics")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }
}
