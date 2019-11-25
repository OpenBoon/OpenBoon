package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class AssetControllerTests : MockMvcTest() {

    @Test
    fun testBatchCreate() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batchCreate")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("assets" to listOf(spec))))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testBatchUpdate() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        created.assets[0].setAttr("test.value", "kirk")

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/assets/_batchUpdate")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("assets" to created.assets)))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testGet() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.assets[0].id

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.document", CoreMatchers.anything()))
            .andReturn()
    }
}