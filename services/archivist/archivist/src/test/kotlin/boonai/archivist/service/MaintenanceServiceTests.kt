package boonai.archivist.service

import boonai.archivist.AbstractTest
import boonai.archivist.repository.TaskDao
import boonai.archivist.domain.AnalystSpec
import boonai.archivist.domain.AnalystState
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.emptyZpsScripts
import boonai.common.service.logging.MeterRegistryHolder
import boonai.common.service.logging.MeterRegistryHolder.meterRegistry
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaintenanceServiceTests : AbstractTest() {

    @Autowired
    lateinit var maintenanceService: MaintenanceService

    @Autowired
    lateinit var analystService: AnalystService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var taskDao: TaskDao

    @Autowired
    lateinit var config: MaintenanceConfiguration

    @Test
    fun testRunAll() {
        maintenanceService.runAll()
    }

    @Test
    fun testRemoveExpiredJobData() {
        maintenanceService.handleExpiredJobs()
    }

    @Test
    fun testHandleUnresponsiveAnalysts() {
        val spec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "unknown",
            null
        )
        authenticateAsAnalyst()
        analystService.upsert(spec)

        jdbc.update(
            "UPDATE analyst SET time_ping=?",
            System.currentTimeMillis() - Duration.parse("PT3M1S").toMillis()
        )
        maintenanceService.handleUnresponsiveAnalysts()

        assertEquals(
            AnalystState.Down.ordinal,
            jdbc.queryForObject("SELECT int_state FROM analyst", Int::class.java)
        )
    }

    @Test
    fun testHandleRemovingtUnresponsiveAnalysts() {
        val spec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "unknown",
            null
        )
        authenticateAsAnalyst()
        analystService.upsert(spec)

        jdbc.update(
            "UPDATE analyst SET int_state=?, time_ping=?",
            AnalystState.Down.ordinal,
            System.currentTimeMillis() - Duration.parse("P1D").toMillis()
        )

        maintenanceService.handleUnresponsiveAnalysts()

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(1) FROM analyst", Int::class.java))
    }

    @Test
    fun testHandleOrphanTasks() {
        val retryCount = meterRegistry.counter(
            "zorroa.task.retry",
            MeterRegistryHolder.getTags()
        ).count()
        val jspec = JobSpec(
            "test_job",
            emptyZpsScripts("test_script"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(jspec)

        val endpoint = "http://localhost:5000"
        val time = System.currentTimeMillis() - (86400 * 1000)
        jdbc.update(
            "UPDATE task SET time_ping=?, int_state=?, str_host=?",
            time, TaskState.Running.ordinal, endpoint
        )
        assertTrue(taskDao.getOrphans(Duration.ofMinutes(1)).isNotEmpty())
        maintenanceService.handleOrphanTasks()
        Thread.sleep(1000)
        val newRetryCount = meterRegistry.counter(
            "zorroa.task.retry",
            MeterRegistryHolder.getTags()
        ).count()
        assertEquals(retryCount + 1, newRetryCount)
    }
}
