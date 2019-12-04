package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.FileCategory
import com.zorroa.archivist.domain.FileGroup
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.storage.FileStorageService
import com.zorroa.archivist.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.File

class AssetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var fileStorageService: FileStorageService

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
    fun testStreamSourceFile() {
        val batchUpload = BatchUploadAssetsRequest(
            assets = listOf(AssetSpec("/foo/bar/toucan.jpg"))
        )
        batchUpload.files = arrayOf(
            MockMultipartFile(
                "files", "toucan.jpg", "image/jpeg",
                File("src/test/resources/test-data/toucan.jpg").inputStream().readAllBytes()
            )
        )

        val rsp = assetService.batchUpload(batchUpload)
        val id = rsp.assets[0].id

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id/_stream")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG_VALUE))
            .andReturn()
    }

    @Test
    fun testSteamFile() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(
            assets=listOf(spec)
        ))
        val id = rsp.assets[0].id
        val loc = FileStorageLocator(FileGroup.ASSET, id, FileCategory.PROXY, "bob.jpg")
        val storage = FileStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        fileStorageService.store(storage)


        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id/files/proxy/bob.jpg")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG_VALUE))
            .andReturn()
    }

    @Test
    fun testUploadFile() {

        val file = MockMultipartFile(
            "file", "toucan.jpg", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readAllBytes()
        )

        val body = MockMultipartFile(
            "body", "",
            "application/json", "{\"name\": \"toucan.jpg\", \"attrs\": {\"foo\": \"bar\"}}".toByteArray()

        )

        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(
            assets=listOf(AssetSpec("https://i.imgur.com/SSN26nN.jpg"))
        ))
        val id = rsp.assets[0].id

        mvc.perform(
            multipart("/api/v3/assets/$id/files/proxy")
                .file(body)
                .file(file)
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.category", CoreMatchers.equalTo("proxy")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name",CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.size",CoreMatchers.equalTo(97221)))
            .andReturn()
    }
}