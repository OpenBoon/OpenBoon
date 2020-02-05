package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.File

class ProjectControllerTests : MockMvcTest() {

    lateinit var testProject: Project
    lateinit var testSpec: ProjectSpec

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Before
    fun init() {
        testSpec = ProjectSpec("Zorroa Test")
        testProject = projectService.create(testSpec)
    }

    @Test
    fun testCreate() {
        val testSpec = ProjectSpec("Zorroa Test2")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/projects")
                .headers(admin())
                .content(Json.serialize(testSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/${testProject.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testProject.name)))
            .andExpect(jsonPath("$.id", CoreMatchers.equalTo(testProject.id.toString())))
            .andExpect(jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetAll() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.list.length()", CoreMatchers.equalTo(2)))
            .andExpect(jsonPath("$.page.totalCount", CoreMatchers.equalTo(2)))
            .andExpect(jsonPath("$.page.from", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun findOne() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/_findOne")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("ids" to listOf(testProject.id))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testProject.name)))
            .andExpect(jsonPath("$.id", CoreMatchers.equalTo(testProject.id.toString())))
            .andExpect(jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun getSettings() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/$pid/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.defaultPipelineId",
                    CoreMatchers.equalTo(settings.defaultPipelineId.toString())
                )
            )
            .andExpect(
                jsonPath(
                    "$.defaultIndexRouteId",
                    CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())
                )
            )
            .andReturn()
    }

    @Test
    fun testSteamFile() {
        val loc = ProjectFileLocator(ProjectStorageEntity.MODEL, "face_v1", "model.txt")
        val storage = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(storage)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/project/_files/model/face_v1/model.txt")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.TEXT_PLAIN))
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
                "entity": "model",
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
            MockMvcRequestBuilders.multipart("/api/v3/project/_files")
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

    @Test
    fun updateSettings() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/projects/$pid/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.defaultPipelineId", CoreMatchers.equalTo(
                settings.defaultPipelineId.toString())))
            .andExpect(jsonPath("$.defaultIndexRouteId",
                CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())))
            .andReturn()
    }

    @Test
    fun testGetMyProject() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("unittest")))
            .andReturn()
    }

    @Test
    fun testGetMyProjectSettings() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.defaultPipelineId", CoreMatchers.anything()))
            .andExpect(jsonPath("$.defaultIndexRouteId", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun updateMySettings() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/project/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.defaultPipelineId", CoreMatchers.equalTo(
                settings.defaultPipelineId.toString())))
            .andExpect(jsonPath("$.defaultIndexRouteId",
                CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())))
            .andReturn()
    }
}
