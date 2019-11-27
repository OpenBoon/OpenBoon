package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Ignore
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.File

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

    @Test
    fun testBatchUpload() {

        val file = MockMultipartFile(
            "files", "file-name.data", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readAllBytes()
        )

        val body = MockMultipartFile(
            "body", "",
            "application/json", "{\"assets\":[{\"uri\": \"src/test/resources/test-data/toucan.jpg\"}]}".toByteArray()
        )

        mvc.perform(
            multipart("/api/v3/assets/_batchUpload")
                .file(body)
                .file(file)
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.assets[0].id", CoreMatchers.anything()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status[0].assetId", CoreMatchers.anything()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status[0].failed", CoreMatchers.equalTo(false)))
    }

    @Test
    @Ignore
    fun testSteamAsset() {
        // TODO: implement this test once the storage part is ready.
    }
}