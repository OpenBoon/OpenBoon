package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.Analyst
import boonai.archivist.domain.AnalystFilter
import boonai.archivist.domain.AnalystSpec
import boonai.archivist.domain.AnalystState
import boonai.archivist.domain.LockState
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnalystDaoTests : AbstractTest() {

    @Autowired
    lateinit var analystDao: AnalystDao

    lateinit var analyst: Analyst
    lateinit var spec: AnalystSpec

    @Before
    fun init() {
        authenticateAsAnalyst()
        spec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "unknown",
            null
        )
        analyst = analystDao.create(spec)
    }

    @Test
    fun testCreate() {
        assertEquals(spec.taskId, analyst.taskId)
        assertEquals(spec.totalRamMb, analyst.totalRamMb)
        assertEquals(spec.freeRamMb, analyst.freeRamMb)
        assertEquals(spec.freeDiskMb, analyst.freeDiskMb)
        assertEquals(spec.load, analyst.load)
    }

    @Test
    fun testUpdate() {
        assertEquals(
            1,
            jdbc.update(
                "UPDATE analyst SET int_state=0 WHERE pk_analyst=?",
                analyst.id
            )
        )
        val spec = AnalystSpec(
            99,
            99,
            99,
            0.1f,
            "hello",
            null
        )
        assertTrue(analystDao.update(spec))
        val a2 = analystDao.get(analyst.id)
        assertEquals(spec.totalRamMb, a2.totalRamMb)
        assertEquals(spec.freeRamMb, a2.freeRamMb)
        assertEquals(spec.freeDiskMb, a2.freeDiskMb)
        assertEquals(spec.load, a2.load)
        assertEquals(AnalystState.Up, a2.state)
    }

    @Test
    fun testGet() {
        val a1 = analystDao.get(analyst.id)
        assertEquals(a1.endpoint, analyst.endpoint)
        assertEquals(a1.taskId, analyst.taskId)
        assertEquals(a1.totalRamMb, analyst.totalRamMb)
        assertEquals(a1.freeRamMb, analyst.freeRamMb)
        assertEquals(a1.freeDiskMb, analyst.freeDiskMb)
        assertEquals(a1.load, analyst.load)
    }

    @Test
    fun testGetByEndpoint() {
        val a1 = analystDao.get(analyst.endpoint)
        assertEquals(a1.endpoint, analyst.endpoint)
        assertEquals(a1.taskId, analyst.taskId)
        assertEquals(a1.totalRamMb, analyst.totalRamMb)
        assertEquals(a1.freeRamMb, analyst.freeRamMb)
        assertEquals(a1.load, analyst.load)
    }

    @Test
    fun testExists() {
        assertTrue(analystDao.exists(analyst.endpoint))
        assertFalse(analystDao.exists("fooo"))
    }

    @Test
    fun testSetState() {
        assertTrue(analystDao.setState(analyst, AnalystState.Down))
        assertFalse(analystDao.setState(analyst, AnalystState.Down))
        assertTrue(analystDao.setState(analyst, AnalystState.Up))
    }

    @Test
    fun testSetLockState() {
        assertTrue(analystDao.setLockState(analyst, LockState.Locked))
        assertFalse(analystDao.setLockState(analyst, LockState.Locked))
        assertTrue(analystDao.setLockState(analyst, LockState.Unlocked))
    }

    @Test
    fun testIsInLockState() {
        assertTrue(analystDao.setLockState(analyst, LockState.Locked))
        assertTrue(analystDao.isInLockState(analyst.endpoint, LockState.Locked))
    }

    @Test
    fun testGetAllById() {
        assertEquals(1, analystDao.getAll(AnalystFilter(ids = listOf(analyst.id))).size())
        assertEquals(0, analystDao.getAll(AnalystFilter(ids = listOf(UUID.randomUUID()))).size())
    }

    @Test
    fun testGetAllByEndpoint() {
        assertEquals(1, analystDao.getAll(AnalystFilter(endpoints = listOf(analyst.endpoint))).size())
        assertEquals(0, analystDao.getAll(AnalystFilter(endpoints = listOf("foo"))).size())
    }

    @Test
    fun testGetAllByStates() {
        assertEquals(1, analystDao.getAll(AnalystFilter(states = listOf(AnalystState.Up))).size())
        assertEquals(0, analystDao.getAll(AnalystFilter(states = listOf(AnalystState.Down))).size())
    }

    @Test
    fun testGetAllByLockState() {
        assertEquals(1, analystDao.getAll(AnalystFilter(lockStates = listOf(LockState.Unlocked))).size())
        assertEquals(0, analystDao.getAll(AnalystFilter(lockStates = listOf(LockState.Locked))).size())
    }

    @Test
    fun testGetAllByTaskId() {
        val spec2 = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "unknown",
            UUID.randomUUID()
        )
        assertTrue(analystDao.update(spec2))
        assertEquals(1, analystDao.getAll(AnalystFilter(taskIds = listOf(spec2.taskId!!))).size())
        assertEquals(0, analystDao.getAll(AnalystFilter(taskIds = listOf(UUID.randomUUID()))).size())
    }

    @Test
    fun testGetAllSorted() {
        authenticateAsAnalyst()
        for (i in 1..10) {
            analystDao.create(
                AnalystSpec(
                    1024,
                    648,
                    1024,
                    i.toFloat(),
                    "unknown",
                    null
                ).apply { endpoint = "http://analyst$i:5000" }
            )
        }
        var last = 0.0f
        for (analyst in analystDao.getAll(AnalystFilter().apply { sort = listOf("load:a") })) {
            assertTrue(analyst.load > last)
            last = analyst.load
        }
        assertTrue(last > 5)
    }

    @Test
    fun testGetUnresponsive() {
        val time = System.currentTimeMillis() - 10000
        jdbc.update("UPDATE analyst SET time_ping=?", time)

        // Get Analyst that hasn't pinged in 1 second
        println(Duration.parse("PT1S").toMillis())
        assertTrue(
            analystDao.getUnresponsive(
                AnalystState.Up,
                Duration.parse("PT1S")
            ).isNotEmpty()
        )
        assertTrue(
            analystDao.getUnresponsive(
                AnalystState.Up,
                Duration.parse("PT20S")
            ).isEmpty()
        )
    }

    @Test
    fun testDelete() {
        assertTrue(analystDao.delete(analyst))
        assertFalse(analystDao.delete(analyst))
    }
}
