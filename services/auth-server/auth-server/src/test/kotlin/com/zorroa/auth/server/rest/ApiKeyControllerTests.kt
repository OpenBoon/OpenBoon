package com.zorroa.auth.server.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zorroa.auth.server.MockMvcTest
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.zmlp.apikey.Permission
import java.util.UUID
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath

class ApiKeyControllerTests : MockMvcTest() {

    val json = jacksonObjectMapper()

    @Test
    fun testCreate() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val pid = UUID.randomUUID()
        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/apikey")
                .headers(superAdmin(pid))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.projectId", CoreMatchers.equalTo(pid.toString())))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("test")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testCreate_rsp_403() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/apikey")
                .headers(standardUser(mockKey))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(spec))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/${mockKey.id}")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val filter = ApiKeyFilter(names = listOf("standard-key"))

        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/_findOne")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testSearch() {
        val filter = ApiKeyFilter(names = listOf("standard-key"))
        filter.sort = listOf("name:asc")

        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/_search")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.list[0].name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.list[0].permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testFindOne_rsp_401() {
        val filter = ApiKeyFilter(names = listOf("mrcatlady"))

        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/_findOne")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(filter))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(401))
            .andReturn()
    }

    @Test
    fun testDownload() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/${mockKey.id}/_download")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().exists("Content-disposition"))
            .andReturn()
    }

    @Test
    fun testGetAll() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.[0]name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.[0]permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testDelete() {
        mvc.perform(
            MockMvcRequestBuilders.delete("/auth/v1/apikey/${mockKey.id}")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
}
