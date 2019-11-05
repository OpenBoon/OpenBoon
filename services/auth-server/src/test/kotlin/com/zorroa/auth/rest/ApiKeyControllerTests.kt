package com.zorroa.auth.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zorroa.auth.MockMvcTest
import com.zorroa.auth.domain.ApiKeySpec
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.util.*

class ApiKeyControllerTests : MockMvcTest() {

    val json = jacksonObjectMapper()

    @Test
    fun testCreate() {
        val spec = ApiKeySpec(
                "test",
                UUID.randomUUID(),
                listOf("foo")
        )

        mvc.perform(
                MockMvcRequestBuilders.post("/auth/v1/apikey")
                        .headers(superAdmin())
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json.writeValueAsBytes(spec))
        )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.projectId", CoreMatchers.equalTo(spec.projectId.toString())))
                .andExpect(jsonPath("$.name", CoreMatchers.equalTo("test")))
                .andExpect(jsonPath("$.permissions[0]",
                        CoreMatchers.containsString("foo")))
                .andReturn()
    }

    @Test
    fun testCreateFailure403() {
        val spec = ApiKeySpec(
                "test",
                UUID.randomUUID(),
                listOf("foo")
        )

        mvc.perform(
                MockMvcRequestBuilders.post("/auth/v1/apikey")
                        .headers(standardUser(standardKey))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(json.writeValueAsBytes(spec))
        )
                .andExpect(MockMvcResultMatchers.status().is4xxClientError)
                .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
                MockMvcRequestBuilders.get("/auth/v1/apikey/${standardKey.keyId}")
                        .headers(superAdmin(standardKey.projectId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.name", CoreMatchers.equalTo("standard-key")))
                .andExpect(jsonPath("$.permissions[0]",
                        CoreMatchers.containsString("Test")))
                .andReturn()
    }

    @Test
    fun testGetAll() {
        mvc.perform(
                MockMvcRequestBuilders.get("/auth/v1/apikey")
                        .headers(superAdmin(standardKey.projectId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(jsonPath("$.[0]name", CoreMatchers.equalTo("standard-key")))
                .andExpect(jsonPath("$.[0]permissions[0]",
                        CoreMatchers.containsString("Test")))
                .andReturn()
    }

    @Test
    fun testDelete() {
        mvc.perform(
                MockMvcRequestBuilders.delete("/auth/v1/apikey/${standardKey.keyId}")
                        .headers(superAdmin(standardKey.projectId))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
    }
}
