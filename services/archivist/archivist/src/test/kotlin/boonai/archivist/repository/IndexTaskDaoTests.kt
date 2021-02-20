package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.IndexTask
import boonai.archivist.domain.IndexTaskState
import boonai.archivist.domain.IndexTaskType
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IndexTaskDaoTests : AbstractTest() {

    @Autowired
    lateinit var indexTaskDao: IndexTaskDao

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    @Autowired
    lateinit var indexTaskJdbcDao: IndexTaskJdbcDao

    fun create(): IndexTask {
        val task = indexTaskDao.save(
            IndexTask(
                UUID.randomUUID(),
                getProjectId(),
                indexRouteDao.getProjectRoute().id,
                null,
                "Test job",
                IndexTaskType.UPDATE,
                IndexTaskState.RUNNING,
                "test update",
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                getZmlpActor().toString(),
                getZmlpActor().toString()
            )

        )
        return indexTaskDao.saveAndFlush(task)
    }

    @Test
    fun testGetAllByState() {
        assertEquals(0, indexTaskDao.getAllByStateOrderByTimeCreatedDesc(IndexTaskState.RUNNING).size)
        assertEquals(0, indexTaskDao.getAllByStateOrderByTimeCreatedDesc(IndexTaskState.FINISHED).size)
    }

    @Test
    fun testUpdateState() {
        val task = create()
        assertTrue(indexTaskJdbcDao.updateState(task, IndexTaskState.FINISHED))
        assertFalse(indexTaskJdbcDao.updateState(task, IndexTaskState.FINISHED))
    }

    @Test
    fun testDeleteExpiredTasks() {
        create()
        assertEquals(
            0,
            indexTaskJdbcDao.deleteExpiredTasks(
                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            )
        )

        assertEquals(
            1,
            indexTaskJdbcDao.deleteExpiredTasks(
                System.currentTimeMillis() + 10000
            )
        )
    }
}
