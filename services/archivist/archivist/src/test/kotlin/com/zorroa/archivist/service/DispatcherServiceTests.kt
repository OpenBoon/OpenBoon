package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AnalystSpec
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchIndexAssetsEvent
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.LockState
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.domain.TaskExpandEvent
import com.zorroa.archivist.domain.TaskProgressUpdateEvent
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.TaskStatsEvent
import com.zorroa.archivist.domain.TaskStatusUpdateEvent
import com.zorroa.archivist.domain.TaskStoppedEvent
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.security.getProjectId
import com.zorroa.zmlp.service.logging.MeterRegistryHolder.meterRegistry
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DispatcherServiceTests : AbstractTest() {

    @Autowired
    lateinit var analystDao: AnalystDao

    @Autowired
    lateinit var taskDao: TaskDao

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var dispatchQueueManager: DispatchQueueManager

    @Test
    fun testHandleStatsEvent() {
        val stats = listOf(TaskStatsEvent("zplugins.core.TestProcessor", 10, 0.1, 0.5, 0.25))
        dispatcherService.handleStatsEvent(stats)

        assertEquals(
            0.1, meterRegistry.timer(
                "zorroa.processor.process_min_time",
                "processor", "zplugins.core.TestProcessor"
            ).totalTime(TimeUnit.SECONDS)
        )

        assertEquals(
            0.5, meterRegistry.timer(
                "zorroa.processor.process_max_time",
                "processor", "zplugins.core.TestProcessor"
            ).totalTime(TimeUnit.SECONDS)
        )

        assertEquals(
            0.25, meterRegistry.timer(
                "zorroa.processor.process_avg_time",
                "processor", "zplugins.core.TestProcessor"
            ).totalTime(TimeUnit.SECONDS)
        )
    }

    @Test
    fun testHandleIndexEventInvalidData() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec)
        val task = dispatcherService.getWaitingTasks(getProjectId(), 1)[0]

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec("gs://cats/large-brown-cat.jpg"),
                AssetSpec("gs://cats/large-brown-cat.mov"),
                AssetSpec("gs://cats/large-brown-cat.pdf")
            ))

        val createRsp = assetService.batchCreate(batchCreate)
        authenticateAsAnalyst()
        val rsp = dispatcherService.handleIndexEvent(task, BatchIndexAssetsEvent(
            mapOf(createRsp.created[0] to mutableMapOf<String, Any>("foo" to "bar")), null))
        assertTrue(rsp?.hasFailures() ?: false)

        authenticate()
        val error = taskErrorDao.getLast()
        assertTrue("introduction of [foo] within [_doc] is not allowed" in error.message)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testHandleIndexEvent() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(
                AssetSpec("gs://cats/large-brown-cat.jpg")
            ))

        val createRsp = assetService.batchCreate(batchCreate)

        jobService.create(spec)
        val task = dispatcherService.getWaitingTasks(getProjectId(), 1)[0]

        authenticateAsAnalyst()
        dispatcherService.handleIndexEvent(task, BatchIndexAssetsEvent(
            mapOf(createRsp.created[0] to
                mutableMapOf<String, Any>(
                    "source" to mutableMapOf("path" to "/cat.jpg"),
                    "media" to mutableMapOf("type" to "image"))
                ), null))

        authenticate()
        // should be no errors
        taskErrorDao.getLast()
    }

    @Test
    fun getTaskPriority() {
        var priority = dispatcherService.getDispatchPriority()
        assertTrue(priority.isEmpty())

        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        jobService.create(spec)

        priority = dispatcherService.getDispatchPriority()
        assertEquals(1, priority.size)

        jdbc.update("UPDATE job_count SET int_task_state_1=100")

        // The org should have priority of 100 now.
        priority = dispatcherService.getDispatchPriority()
        assertEquals(100, priority[0].priority)
    }

    @Test
    fun getTaskPriorityMultipleProjects() {

        val spec1 = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec1)
        jdbc.update(
            "UPDATE job_count SET int_task_state_1=100 WHERE pk_job=?",
            job.id
        )

        val pspec = ProjectSpec("foojam", id = UUID.randomUUID())
        val project = projectService.create(pspec)
        authenticate(project.id)

        val spec2 = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        jobService.create(spec2)

        val priority = dispatcherService.getDispatchPriority()
        assertEquals(0, priority[0].priority)
        assertEquals(100, priority[1].priority)
    }

    @Test
    fun testGetNextWithInteractivePriortity() {
        val analyst = "http://127.0.0.1:5000"

        // Standard job is launched first, which should go first
        launchJob(JobPriority.Standard)
        Thread.sleep(2)
        // A higher priority job is launched, now it goes first.
        val job = launchJob(JobPriority.Interactive)

        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertEquals(job.id, it.jobId)
            val host: String = this.jdbc.queryForObject(
                "SELECT str_host FROM task WHERE pk_task=?",
                String::class.java, it.id
            )
            assertEquals(analyst, host)
        }
    }

    @Test
    fun testGetNext() {
        val analyst = "http://127.0.0.1:5000"
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)

        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()

        assertNotNull(next)
        next?.let {
            assertFalse("ZMLP_DATASOURCE_ID" in next.env)
            assertEquals(job.id, it.jobId)
            val host: String = this.jdbc.queryForObject(
                "SELECT str_host FROM task WHERE pk_task=?",
                String::class.java, it.id
            )
            assertEquals(analyst, host)
        }
    }

    @Test
    fun testGetNextFailureMaxRunningJob() {
        val analyst = "http://127.0.0.1:5000"
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar"),
            maxRunningTasks = 0
        )
        jobService.create(spec)
        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()
        assertNull(next)
    }

    @Test
    fun testGetNextLockedAnalyst() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo")
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "http://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val node = analystDao.create(aspec)
        assertTrue(analystDao.setLockState(node, LockState.Locked))

        val next = dispatchQueueManager.getNext()
        assertNull(next)
    }

    @Test
    fun testStartAndStopTask() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo")
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "http://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val node = analystDao.create(aspec)

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertNotNull(analystDao.get(analyst).taskId)
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(0)))
            assertNull(analystDao.get(analyst).taskId)
        }
    }

    // make run
    @Test
    fun testStopErrorTaskManualKill() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Asset(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Asset(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec(
            "test_job",
            ZpsScript(
                "foo",
                generate = null,
                execute = null,
                assets = listOf(doc1, doc2)
            )
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "http://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(-9, manualKill = true)))
            authenticate()
            assertEquals(TaskState.Failure, taskDao.get(next.taskId).state)
        }
    }

    @Test
    fun testStopErrorAutoRetry() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Asset(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Asset(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec(
            "test_job",
            ZpsScript(
                "foo",
                generate = null,
                execute = null,
                assets = listOf(doc1, doc2)
            )
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "http://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(9, manualKill = false)))
            authenticate()
            assertEquals(TaskState.Waiting, taskDao.get(next.taskId).state)
        }
    }

    @Test
    fun testStopErrorPastAutoRetryLimit() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Asset(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Asset(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec(
            "test_job",
            ZpsScript(
                "foo",
                generate = null,
                execute = null,
                assets = listOf(doc1, doc2)
            )
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "http://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            // Set run count above the retry limit.
            jdbc.update("UPDATE task SET int_run_count=4 WHERE pk_task=?", next.taskId)
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(9, manualKill = false)))
            authenticate()
            assertEquals(TaskState.Failure, taskDao.get(next.taskId).state)
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter(jobIds = listOf(next.jobId))).size())
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter(taskIds = listOf(next.taskId))).size())
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter()).size())
        }
    }

    @Test
    fun testExpandFromParentTask() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec)
        val zps = emptyZpsScript("bar")
        zps.execute = mutableListOf(ProcessorRef("foo", "bar"))

        val task1 = dispatcherService.getWaitingTasks(getProjectId(), 1)
        val task2 = dispatcherService.expand(
            task1[0],
            TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg")))
        )
        assertNotNull(task2)
        task2?.id.let {
            val zps2 = taskDao.getScript(it)
            assertNotNull(zps2.execute)
            // Validate task2 inherited from task
            assertEquals(1, zps.execute!!.size)
        }
    }

    @Test
    fun testUpdateProgress() {
        launchJob(JobPriority.Standard)
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        dispatcherService.handleProgressUpdateEvent(task!!, TaskProgressUpdateEvent(75))
        val get = taskDao.get(task.id)

        assertEquals(task.id, get.id)
        assertEquals(75, get.progress)
    }

    @Test
    fun testUpdateStatus() {
        launchJob(JobPriority.Standard)
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        dispatcherService.handleStatusUpdateEvent(task!!, TaskStatusUpdateEvent("testingStatus"))
        val get = taskDao.get(task.id)

        assertEquals(task.id, get.id)
        assertEquals("testingStatus", get.status)
    }

    fun launchJob(priority: Int): Job {
        val spec1 = JobSpec(
            "test_job_p$priority",
            emptyZpsScript("priority_$priority"),
            priority = priority
        )
        return jobService.create(spec1)
    }
}
