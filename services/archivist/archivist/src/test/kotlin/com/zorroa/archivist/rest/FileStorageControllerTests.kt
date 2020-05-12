package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.domain.DataSetType
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.DataSetService
import com.zorroa.archivist.storage.ProjectStorageService
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

class FileStorageControllerTests : MockMvcTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var dataSetService: DataSetService

    @Test
    fun testSteamFile() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS,
            id, ProjectStorageCategory.PROXY, "bob.jpg")
        val storage = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(storage)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/files/_stream/assets/$id/proxy/bob.jpg")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE))
            .andReturn()
    }

    @Test
    fun testGetSignedUploadUri() {

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val payload = """
            {
                "entityId": "$id",
                "entity": "assets",
                "category": "image",
                "name": "toucan.jpg",
                "attrs": {
                    "foo": "bar"
                }
            }
        """.trimIndent()
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/files/_signed_upload_uri")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(job())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.uri", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testSetAttrs() {

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val file = MockMultipartFile(
            "file", "toucan.jpg", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val payload = """
            {
                "entityId": "$id",
                "entity": "assets",
                "category": "image",
                "name": "toucan.jpg",
                "attrs": {
                    "foo": "bar"
                }
            }
        """.trimIndent()

        val body = MockMultipartFile(
            "body", "",
            "application/json",
            payload.toByteArray()
        )

        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v3/files/_upload")
                .file(body)
                .file(file)
                .headers(job())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category", CoreMatchers.equalTo("image")))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(jsonPath("$.size", CoreMatchers.equalTo(97221)))
            .andReturn()

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/files/_attrs")
                .content(payload)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(job())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category", CoreMatchers.equalTo("image")))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(jsonPath("$.size", CoreMatchers.equalTo(97221)))
            .andReturn()
    }

    @Test
    fun testUploadFileToAsset() {

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val file = MockMultipartFile(
            "file", "toucan.jpg", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val payload = """
            {
                "entityId": "$id",
                "entity": "assets",
                "category": "image",
                "name": "toucan.jpg",
                "attrs": {
                    "foo": "bar"
                }
            }
        """.trimIndent()

        val body = MockMultipartFile(
            "body", "",
            "application/json",
            payload.toByteArray()
        )

        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v3/files/_upload")
                .file(body)
                .file(file)
                .headers(job())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category", CoreMatchers.equalTo("image")))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(jsonPath("$.size", CoreMatchers.equalTo(97221)))
            .andReturn()
    }

    @Test
    fun testUploadFileToDataSet() {

        val ds = dataSetService.create(DataSetSpec("foo", DataSetType.LABEL_DETECTION))
        val file = MockMultipartFile(
            "file", "toucan.jpg", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val payload = """
            {
                "entityId": "${ds.id}",
                "entity": "datasets",
                "category": "image",
                "name": "toucan.jpg",
                "attrs": {
                    "foo": "bar"
                }
            }
        """.trimIndent()

        val body = MockMultipartFile(
            "body", "",
            "application/json",
            payload.toByteArray()
        )

        mvc.perform(
            MockMvcRequestBuilders.multipart("/api/v3/files/_upload")
                .file(body)
                .file(file)
                .headers(job())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category", CoreMatchers.equalTo("image")))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(jsonPath("$.size", CoreMatchers.equalTo(97221)))
            .andReturn()
    }
}
