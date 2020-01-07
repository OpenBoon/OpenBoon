package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProjectControllerTests : MockMvcTest() {

    lateinit var testProject: Project
    lateinit var testSpec: ProjectSpec

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
            .andExpect(jsonPath("$.defaultPipelineId",
                CoreMatchers.equalTo(settings.defaultPipelineId.toString())))
            .andExpect(jsonPath("$.defaultIndexRouteId",
                CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())))
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
}