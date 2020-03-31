package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetFileLocator
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.archivist.storage.ProjectStorageService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

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
        val loc = AssetFileLocator(id, ProjectStorageCategory.PROXY, "bob.jpg")
        val storage = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(storage)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/files/assets/$id//proxy/bob.jpg")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG_VALUE))
            .andReturn()
    }
}
