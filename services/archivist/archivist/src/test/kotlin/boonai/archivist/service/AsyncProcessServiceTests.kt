package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AsyncProcessSpec
import boonai.archivist.domain.AsyncProcessType
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.repository.AsyncProcessDao
import boonai.archivist.security.getProjectId
import boonai.archivist.storage.ProjectStorageException
import boonai.archivist.storage.ProjectStorageService
import boonai.common.service.storage.SystemStorageException
import boonai.common.service.storage.SystemStorageService
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertNotNull

class AsyncProcessServiceTests : AbstractTest() {

    @Autowired
    lateinit var asyncProcessService: AsyncProcessService

    @Autowired
    lateinit var asyncProcessHandler: AsyncProcessHandler

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testProjectStorageDelete() {

        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            "1234",
            ProjectStorageCategory.SOURCE,
            "bob.txt"
        )

        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(spec)
        asyncProcessService.createAsyncProcess(
            AsyncProcessSpec(
                getProjectId(),
                "Delete some stuff",
                AsyncProcessType.DELETE_PROJECT_STORAGE
            )
        )

        asyncProcessHandler.handleNextAsyncProcess()

        assertThrows<ProjectStorageException> { projectStorageService.fetch(loc) }
    }

    @Test
    fun testDeleteSystemStorage() {

        systemStorageService.storeObject(
            "projects/${getProjectId()}/test1.json",
            mapOf("foo1" to "bar1")
        )

        assertNotNull(
            systemStorageService.fetchObject(
                "projects/${getProjectId()}/test1.json",
                Map::class.java
            )
        )

        asyncProcessService.createAsyncProcess(
            AsyncProcessSpec(
                getProjectId(),
                "Delete some stuff",
                AsyncProcessType.DELETE_SYSTEM_STORAGE
            )
        )

        assertThrows<SystemStorageException> {
            systemStorageService.fetchObject(
                "projects/${getProjectId()}/test2.json",
                Map::class.java
            )
        }
    }
}
