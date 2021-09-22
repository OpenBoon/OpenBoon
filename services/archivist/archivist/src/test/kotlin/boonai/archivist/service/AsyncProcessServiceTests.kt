package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AsyncProcessSpec
import boonai.archivist.domain.AsyncProcessState
import boonai.archivist.domain.AsyncProcessType
import boonai.archivist.domain.EntityNotFoundException
import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageCategory
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.mock.zany
import boonai.archivist.repository.AsyncProcessDao
import boonai.archivist.repository.AsyncProcessJdbcDao
import boonai.archivist.security.getProjectId
import boonai.archivist.storage.ProjectStorageException
import boonai.archivist.storage.ProjectStorageService
import boonai.common.service.storage.SystemStorageException
import boonai.common.service.storage.SystemStorageService
import com.nhaarman.mockito_kotlin.doThrow
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.queryForObject
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AsyncProcessServiceTests : AbstractTest() {

    @Autowired
    lateinit var asyncProcessService: AsyncProcessService

    @Autowired
    lateinit var asyncProcessHandler: AsyncProcessHandler

    @Autowired
    lateinit var asyncProcessJdbcDao: AsyncProcessJdbcDao

    @Autowired
    lateinit var asyncProcessDao: AsyncProcessDao

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

    @Test
    fun testAsyncProcessExceptionHandler() {

        val indexRoutingMock = Mockito.mock(IndexRoutingService::class.java)
        ReflectionTestUtils.setField(asyncProcessHandler, "indexRoutingService", indexRoutingMock)

        doThrow(
            EntityNotFoundException("Test")
        ).`when`(indexRoutingMock)
            .closeAndDeleteProjectIndexes(zany(UUID::class.java))

        val createAsyncProcess = asyncProcessService.createAsyncProcess(
            AsyncProcessSpec(
                getProjectId(),
                "Delete some stuff",
                AsyncProcessType.DELETE_PROJECT_INDEXES
            )
        )
        asyncProcessHandler.handleNextAsyncProcess()

        val errorState = jdbc.queryForObject(
            "select int_state from process where pk_process='${createAsyncProcess.id}'",
            AsyncProcessState::class.java
        )
        assertEquals(AsyncProcessState.ERROR, errorState)
    }

    @Test
    fun testWatchDogOrphanTask() {
        val createAsyncProcess = asyncProcessService.createAsyncProcess(
            AsyncProcessSpec(
                getProjectId(),
                "Delete some stuff",
                AsyncProcessType.DELETE_PROJECT_INDEXES
            )
        )
        entityManager.flush()

        jdbc.update(
            "UPDATE process SET int_state=?, time_refresh=?  WHERE pk_process=?",
            AsyncProcessState.RUNNING.ordinal,
            System.currentTimeMillis() - (60000 * 10),
            createAsyncProcess.id
        )

        val oldState = jdbc.queryForObject(
            "SELECT int_state FROM process where pk_process=?",
            AsyncProcessState::class.java,
            createAsyncProcess.id
        )
        asyncProcessHandler.runWatchDog()
        entityManager.flush()

        val watchedAsyncProcess = asyncProcessDao.getOne(createAsyncProcess.id)

        assertEquals(AsyncProcessState.RUNNING, oldState)
        assertEquals(AsyncProcessState.PENDING, watchedAsyncProcess.state)
    }
}
