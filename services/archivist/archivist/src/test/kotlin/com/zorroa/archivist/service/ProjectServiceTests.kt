package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectNameUpdate
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.domain.ProjectTier
import com.zorroa.archivist.security.getProjectId

import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectServiceTests : AbstractTest() {

    val testSpec = ProjectSpec("project_test")

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    fun createProject() {
        val project = projectService.create(testSpec)
        assertEquals(testSpec.name, project.name)
    }

    @Test
    fun getProjectById() {
        val project1 = projectService.create(testSpec)
        val project2 = projectService.get(project1.id)
        assertEquals(project1, project2)
    }

    @Test
    fun getProjectByName() {
        val project1 = projectService.create(testSpec)
        val project2 = projectService.get(project1.name)
        assertEquals(project1, project2)
    }

    @Test
    fun getAll() {
        val project = projectService.create(testSpec)
        val result = projectService.getAll(ProjectFilter(ids = listOf(project.id)))
        assertEquals(1, result.size())
    }

    @Test
    fun findOne() {
        val project1 = projectService.create(testSpec)
        val project2 = projectService.findOne(ProjectFilter(ids = listOf(project1.id)))
        assertEquals(project1, project2)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun findOneNotFound() {
        projectService.findOne(ProjectFilter(ids = listOf(UUID.randomUUID())))
    }

    fun testGetSettings() {
        projectService.findOne(ProjectFilter(ids = listOf(UUID.randomUUID())))
    }

    fun testUpdateSettings() {
        val settings = projectService.getSettings(getProjectId())
        val pipeline = pipelineService.create(PipelineSpec("dogs"))

        val mapping = properties.getString("archivist.es.default-mapping-type")
        val ver = properties.getInt("archivist.es.default-mapping-version")

        val index = indexRoutingService.createIndexRoute(
            IndexRouteSpec(
                mapping, ver, projectId = project.id
            )
        )
        settings.defaultPipelineId = pipeline.id
        settings.defaultIndexRouteId = index.id

        assertTrue(projectService.updateSettings(getProjectId(), settings))
        val updated = projectService.getSettings(getProjectId())

        assertEquals(index.id, updated.defaultIndexRouteId)
        assertEquals(pipeline.id, updated.defaultPipelineId)
    }

    fun testUpdateSettingsInvalidIndexRoute() {
        val settings = projectService.getSettings(getProjectId())
        settings.defaultIndexRouteId = UUID.randomUUID()
        projectService.updateSettings(getProjectId(), settings)
    }

    fun testUpdateSettingsInvalidPipeline() {
        val settings = projectService.getSettings(getProjectId())
        settings.defaultPipelineId = UUID.randomUUID()
        projectService.updateSettings(getProjectId(), settings)
    }

    @Test
    fun testGetCryptoKey() {
        val key = projectService.getCryptoKey()
        assertEquals(99, key.length)
    }

    @Test
    fun testSetEnable() {
        val testSpec = ProjectSpec("project_test")
        val project1 = projectService.create(testSpec)
        projectService.setEnabled(project1.id, false)

        var status = jdbc.queryForObject(
            "SELECT enabled FROM project WHERE pk_project=?", Boolean::class.java, project1.id
        )
        assertFalse(status)

        projectService.setEnabled(project1.id, true)
        status = jdbc.queryForObject(
            "SELECT enabled FROM project WHERE pk_project=?", Boolean::class.java, project1.id
        )
        assertTrue(status)
    }

    @Test
    fun testSetTier() {
        val testSpec = ProjectSpec("project_test")
        val project = projectService.create(testSpec)
        assertEquals(ProjectTier.ESSENTIALS, project.tier)

        projectService.setTier(project.id, ProjectTier.PREMIUM)
        var tierOrdinal = jdbc.queryForObject(
            "SELECT int_tier FROM project WHERE pk_project=?", Int::class.java, project.id
        )

        assertEquals(ProjectTier.PREMIUM, ProjectTier.values()[tierOrdinal])
    }

    @Test
    fun testRename(){
        val testSpec = ProjectSpec("project_test")
        val project = projectService.create(testSpec)
        assertEquals("project_test", project.name)

        projectService.rename(project.id, ProjectNameUpdate("project_test_renamed"));
        var newName = jdbc.queryForObject(
            "SELECT str_name FROM project WHERE pk_project=?", String::class.java, project.id
        )

        assertEquals("project_test_renamed", newName)
    }
}
