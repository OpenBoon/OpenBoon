package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals

@WebAppConfiguration
class AnalystClusterControllerTests : MockMvcTest() {

    @Test
    fun testPing() {
        SecurityContextHolder.getContext().authentication = null

        val spec = AnalystSpec(
                "http://localhost:1234",
                null,
                1024,
                648,
                0.5f)

        val result = mvc.perform(MockMvcRequestBuilders.post("/cluster/analysts")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val analyst = Json.Mapper.readValue<Analyst>(result.response.contentAsString, Analyst::class.java)
        assertEquals(spec.endpoint, analyst.endpoint)
    }
}