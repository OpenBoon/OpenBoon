package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.archivist.storage.ProjectStorageService
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

class FileStorageControllerTests : MockMvcTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Test
    fun testSteamFile() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSET,
            id, ProjectStorageCategory.PROXY, "bob.jpg")
        val storage = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(storage)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/files/_stream/assets/$id/proxy/bob.jpg")
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
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val payload = """
            {
                "entityId": "12345",
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
                .headers(admin())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.category", CoreMatchers.equalTo("image")))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(jsonPath("$.size", CoreMatchers.equalTo(97221)))
            .andReturn()
    }
}
