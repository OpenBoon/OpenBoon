package boonai.archivist.queue.listener

import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.queue.PubSubAbstractTest
import boonai.archivist.security.getProjectId
import boonai.archivist.storage.ProjectStorageException
import boonai.archivist.storage.ProjectStorageService
import boonai.common.service.storage.SystemStorageException
import boonai.common.service.storage.SystemStorageService
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ProjectListenerTest : PubSubAbstractTest() {

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Test
    fun testDeleteProject() {
        val projectId = getProjectId()

        sendTestMessage(
            projectListener,
            "delete",
            projectId.toString()
        )

        val listOfTables = listOf(
            "project_quota",
            "project_quota_time_series",
            "processor",
            "module",
            "credentials",
            "pipeline",
            "automl",
            "model",
            "job",
            "datasource",
            "project"

        )
        listOfTables.forEach {
            assertEquals(
                0,
                jdbc.queryForObject("SELECT COUNT(*) FROM $it where pk_project=?", Int::class.java, getProjectId())
            )
        }
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

        sendTestMessage(
            projectListener,
            "system-storage/delete",
            getProjectId().toString()
        )

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
    fun testDeleteStorage() {

        val loc = ProjectFileLocator(ProjectStorageEntity.ASSETS, "1234", ProjectStorageCategory.SOURCE, "bob.txt")
        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        val result = projectStorageService.store(spec)

        sendTestMessage(
            projectListener,
            "storage/delete",
            getProjectId().toString()
        )

        assertEquals(true, result.size > 0)
        assertThrows<ProjectStorageException> {
            projectStorageService.fetch(loc)
        }
    }
}
