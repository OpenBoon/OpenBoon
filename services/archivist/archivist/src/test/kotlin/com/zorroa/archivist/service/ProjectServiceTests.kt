package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSpec

import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import java.util.UUID
import kotlin.test.assertEquals

class ProjectServiceTests : AbstractTest() {

    val testSpec = ProjectSpec("project_test")

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
}