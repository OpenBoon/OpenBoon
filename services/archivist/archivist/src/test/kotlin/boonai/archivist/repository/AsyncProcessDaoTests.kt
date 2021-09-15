package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.AsyncProcessSpec
import boonai.archivist.domain.AsyncProcessState
import boonai.archivist.domain.AsyncProcessType
import boonai.archivist.service.AsyncProcessService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AsyncProcessDaoTests : AbstractTest() {

    @Autowired
    lateinit var asyncProcessDao: AsyncProcessDao

    @Autowired
    lateinit var asyncProcessJdbcDao: AsyncProcessJdbcDao

    @Autowired
    lateinit var asyncProcessService: AsyncProcessService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testFindTopByStateOrderByTimeCreatedAscEmpty() {
        assertNull(asyncProcessDao.findTopByStateOrderByTimeCreatedAsc(AsyncProcessState.PENDING))
    }

    @Test
    fun testFindTopByStateOrderByTimeCreatedAsc() {
        val spec = AsyncProcessSpec(UUID.randomUUID(), "Delete some stuff", AsyncProcessType.DELETE_PROJECT_STORAGE)
        val proc = asyncProcessService.createAsyncProcess(spec)
        val top = asyncProcessDao.findTopByStateOrderByTimeCreatedAsc(AsyncProcessState.PENDING)
        assertEquals(proc.id, top?.id)
    }

    @Test
    fun testUpdateStateWithOldState() {
        val spec = AsyncProcessSpec(UUID.randomUUID(), "Delete some stuff", AsyncProcessType.DELETE_PROJECT_STORAGE)
        val proc = asyncProcessService.createAsyncProcess(spec)
        entityManager.flush()

        assertTrue(asyncProcessJdbcDao.setState(proc, AsyncProcessState.RUNNING, AsyncProcessState.PENDING))
        assertFalse(asyncProcessJdbcDao.setState(proc, AsyncProcessState.RUNNING, AsyncProcessState.PENDING))
        assertTrue(asyncProcessJdbcDao.setState(proc, AsyncProcessState.SUCCESS, AsyncProcessState.RUNNING))
    }

    @Test
    fun testUpdateState() {
        val spec = AsyncProcessSpec(UUID.randomUUID(), "Delete some stuff", AsyncProcessType.DELETE_PROJECT_STORAGE)
        val proc = asyncProcessService.createAsyncProcess(spec)
        entityManager.flush()

        assertTrue(asyncProcessJdbcDao.setState(proc, AsyncProcessState.RUNNING))
        assertFalse(asyncProcessJdbcDao.setState(proc, AsyncProcessState.RUNNING))
        assertTrue(asyncProcessJdbcDao.setState(proc, AsyncProcessState.PENDING))
    }

    @Test
    fun updateRefreshTime() {
        val spec = AsyncProcessSpec(UUID.randomUUID(), "Delete some stuff", AsyncProcessType.DELETE_PROJECT_STORAGE)
        val proc = asyncProcessService.createAsyncProcess(spec)
        entityManager.flush()

        Thread.sleep(200)
        assertTrue(asyncProcessJdbcDao.updateRefreshTime(proc.id))
        entityManager.clear()

        val proc2 = asyncProcessDao.getOne(proc.id)
        assertTrue(proc2.timeRefresh > proc.timeRefresh)
    }
}
