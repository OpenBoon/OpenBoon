package boonai.authserver.rest

import boonai.authserver.MockMvcTest
import org.junit.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@AutoConfigureMockMvc
class ActuatorTests : MockMvcTest() {

    @Test
    fun testInfoEndpoint() {
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/info")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testHealthEndpoint() {
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/health")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
            .andReturn()
    }

    @Test
    fun testMetrics() {
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/metrics")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .with(httpBasic("monitor", "monitor"))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.names").exists())
            .andReturn()
    }

    @Test
    fun testMetrics_rsp_400() {
        mvc.perform(
            MockMvcRequestBuilders.get("/monitor/metrics")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }
}
