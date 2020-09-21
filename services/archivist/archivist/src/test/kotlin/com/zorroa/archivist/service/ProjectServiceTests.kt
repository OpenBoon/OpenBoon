package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectNameUpdate
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.ProjectTier
import com.zorroa.archivist.repository.IndexRouteDao
import com.zorroa.archivist.security.getProjectId
import com.zorroa.zmlp.service.storage.SystemStorageException
import com.zorroa.zmlp.service.storage.SystemStorageService
import com.zorroa.archivist.storage.ProjectStorageException
import com.zorroa.archivist.storage.ProjectStorageService

import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProjectServiceTests : AbstractTest() {

    val testSpec = ProjectSpec("project_test")

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

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

    @Test
    fun testGetCryptoKey() {
        val key = projectService.getCryptoKey()
        assertEquals(99, key.length)
    }

    @Test
    fun testEnableProject() {
        val project1 = projectService.create(testSpec)

        val status = jdbc.queryForObject(
            "SELECT enabled FROM project WHERE pk_project=?", Boolean::class.java, project.id
        )
        assertTrue(status)

        // Check Project Routes
        projectService.setEnabled(project.id, false)
        projectService.setEnabled(project.id, true)

        var route = indexRouteDao.getProjectRoute()
        val esIndexState = indexRoutingService.getEsIndexState(route)
        assertEquals("open", esIndexState["status"])
    }

    @Test
    fun testDisableProject() {
        projectService.setEnabled(project.id, false)

        var status = jdbc.queryForObject(
            "SELECT enabled FROM project WHERE pk_project=?", Boolean::class.java, project.id
        )
        assertFalse(status)

        val state = indexRoutingService.getEsIndexState(indexRouteDao.getProjectRoute())
        assertEquals("close", state["status"])
    }

    @Test
    fun testSetTier() {
        val testSpec = ProjectSpec("project_test")
        val project = projectService.create(testSpec)
        assertEquals(ProjectTier.ESSENTIALS, project.tier)

        projectService.setTier(project.id, ProjectTier.PREMIER)
        var tierOrdinal = jdbc.queryForObject(
            "SELECT int_tier FROM project WHERE pk_project=?", Int::class.java, project.id
        )

        assertEquals(ProjectTier.PREMIER, ProjectTier.values()[tierOrdinal])
    }

    @Test
    fun testRename() {
        val testSpec = ProjectSpec("project_test")
        val project = projectService.create(testSpec)
        assertEquals("project_test", project.name)

        projectService.rename(project.id, ProjectNameUpdate("project_test_renamed"))
        var newName = jdbc.queryForObject(
            "SELECT str_name FROM project WHERE pk_project=?", String::class.java, project.id
        )

        assertEquals("project_test_renamed", newName)
    }

    @Test
    fun testDeleteSystemStorage() {

        systemStorageService.storeObject(
            "projects/${getProjectId()}/test1.json",
            mapOf("foo1" to "bar1")
        )
        systemStorageService.storeObject(
            "projects/${getProjectId()}/test2.json",
            mapOf("foo2" to "bar2")
        )

        val obj1 = systemStorageService.fetchObject("projects/${getProjectId()}/test1.json", Map::class.java)
        val obj2 = systemStorageService.fetchObject("projects/${getProjectId()}/test2.json", Map::class.java)

        projectService.deleteProjectSystemStorage(project)

        assertEquals(true, obj1.isNotEmpty())
        assertEquals(true, obj2.isNotEmpty())
        assertThrows<SystemStorageException> {
            systemStorageService.fetchObject("projects/${getProjectId()}/test1.json", Map::class.java)
        }
        assertThrows<SystemStorageException> {
            systemStorageService.fetchObject("projects/${getProjectId()}/test2.json", Map::class.java)
        }
    }

    @Test
    fun testDeleteProjectStorageFiles() {
        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)
        projectService.deleteProjectStorage(project)

        assertEquals(true, result.size > 0)
        assertThrows<ProjectStorageException> {
            projectStorageService.fetch(loc)
        }
    }
}
