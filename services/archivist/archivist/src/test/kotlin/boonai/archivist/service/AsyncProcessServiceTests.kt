package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AsyncProcess
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
import org.junit.Ignore
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.Phaser
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.concurrent.thread

class AsyncProcessServiceTests : AbstractTest() {

    @Autowired
    lateinit var asyncProcessService: AsyncProcessService

    @Autowired
    lateinit var asyncProcessDao: AsyncProcessDao

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var systemStorageService: SystemStorageService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    private fun projectFileLocator(): ProjectFileLocator {
        val loc = ProjectFileLocator(
            ProjectStorageEntity.ASSETS,
            "1234",
            ProjectStorageCategory.SOURCE,
            "bob.txt"
        )
        return loc
    }

    // To run these tests, remove @Ignore and set archivist.async-process.enabled = true
    @Test
    @Ignore
    fun testProjectStorageDelete() {
        var createAsyncProcess: AsyncProcess? = null
        runTestingThread(
            prepareTest = {
                val loc = projectFileLocator()
                val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
                projectStorageService.store(spec)
                createAsyncProcess = asyncProcessService.createAsyncProcess(
                    AsyncProcessSpec(
                        getProjectId(),
                        "Delete some stuff",
                        AsyncProcessType.DELETE_PROJECT_STORAGE
                    )
                )
            },
            evaluate = {
                assertThrows<ProjectStorageException> {
                    projectStorageService.fetch(projectFileLocator())
                }
            },
            cleanUp = {
                createAsyncProcess?.let {
                    asyncProcessDao.delete(it)
                }
            }
        )
    }

    @Test
    @Ignore
    fun testDeleteSystemStorage() {
        var createAsyncProcess: AsyncProcess? = null

        runTestingThread(
            prepareTest = {
                systemStorageService.storeObject(
                    "projects/${getProjectId()}/test1.json",
                    mapOf("foo1" to "bar1")
                )
                systemStorageService.storeObject(
                    "projects/${getProjectId()}/test2.json",
                    mapOf("foo2" to "bar2")
                )
                systemStorageService.fetchObject(
                    "projects/${getProjectId()}/test1.json",
                    Map::class.java
                )
                systemStorageService.fetchObject(
                    "projects/${getProjectId()}/test2.json",
                    Map::class.java
                )

                createAsyncProcess = asyncProcessService.createAsyncProcess(
                    AsyncProcessSpec(
                        getProjectId(),
                        "Delete some stuff",
                        AsyncProcessType.DELETE_SYSTEM_STORAGE
                    )
                )
            },
            evaluate = {
                assertThrows<SystemStorageException> {
                    systemStorageService.fetchObject(
                        "projects/${getProjectId()}/test1.json",
                        Map::class.java
                    )
                }
                assertThrows<SystemStorageException> {
                    systemStorageService.fetchObject(
                        "projects/${getProjectId()}/test2.json",
                        Map::class.java
                    )
                }
            },
            cleanUp = {
                createAsyncProcess?.let {
                    asyncProcessDao.delete(it)
                }
            }
        )
    }

    fun runTestingThread(prepareTest: () -> Unit, evaluate: () -> Unit, cleanUp: () -> Unit) {
        var error: AssertionError? = null
        val ph = Phaser(2)
        thread {
            authenticate()
            prepareTest()
            try {
                Thread.sleep(1000)
                evaluate()
            } catch (ex: AssertionError) {
                error = ex
            } finally {
                cleanUp()
                ph.arrive()
            }
        }.run()
        ph.arriveAndAwaitAdvance()
        error?.let { throw (it) }
    }
}
