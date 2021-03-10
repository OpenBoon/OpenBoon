package boonai.archivist.rest

import boonai.archivist.MockMvcTest
import boonai.archivist.domain.AssetSpec
import boonai.archivist.domain.BatchCreateAssetsRequest
import boonai.archivist.domain.ModelSpec
import boonai.archivist.domain.ModelType
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.service.ModelService
import boonai.archivist.storage.ProjectStorageService
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
    lateinit var modelService: ModelService

    @Test
    fun testSteamFile() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            id, ProjectStorageCategory.PROXY, "bob.jpg"
        )
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
        val payload =
            """
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
    fun testGetSignedDownloadUri() {

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            id, ProjectStorageCategory.PROXY, "bob.jpg"
        )
        val storage = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(storage)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/files/_sign/assets/$id/proxy/bob.jpg")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.mediaType", CoreMatchers.equalTo("image/jpeg")))
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

        val payload =
            """
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

        val payload =
            """
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
    fun testUploadLabeledModel() {

        val ds = modelService.createModel(ModelSpec("foo", ModelType.TF_CLASSIFIER))
        val file = MockMultipartFile(
            "file", "toucan.jpg", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val payload =
            """
            {
                "entityId": "${ds.id}",
                "entity": "models",
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
