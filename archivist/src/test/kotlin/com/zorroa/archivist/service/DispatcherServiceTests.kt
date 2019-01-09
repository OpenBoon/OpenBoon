package com.zorroa.archivist.service

import com.nhaarman.mockito_kotlin.whenever
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.mock.zany
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.common.domain.AnalystSpec
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.LockState
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestPropertySource(locations=["classpath:gcs-test.properties"])
class GCPDispatcherServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var fileStorageService: FileStorageService

    @Test
    fun testGetNext() {

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        jobService.create(spec)

        val storage = FileStorage(
                "foo", "gs://foo/bar/bing.jpg", "fs", "image/jpeg", fileServerProvider)

        whenever(fileStorageService.get(zany(FileStorageSpec::class.java))).thenReturn(storage)
        whenever(fileStorageService.getSignedUrl(zany(), zany(), anyLong(), zany())).thenReturn("https://foo/bar")

        authenticateAsAnalyst()
        val next = dispatcherService.getNext()
        assertNotNull(next)
        assertEquals(next?.logFile, "https://foo/bar")
    }
}

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

    @Test
    fun testGetNext() {
        val analyst = "https://127.0.0.1:5000"
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)

        authenticateAsAnalyst()
        val next = dispatcherService.getNext()
        assertNotNull(next)
        next?.let {
            assertEquals(job.id, it.jobId)
            val host :String = this.jdbc.queryForObject("SELECT str_host FROM task WHERE pk_task=?",
                    String::class.java, it.id)
            assertEquals(analyst, host)
        }
    }

    @Test
    fun testGetNextLockedAnalyst() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"))
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                null).apply { endpoint = analyst }

        val node = analystDao.create(aspec)
        assertTrue(analystDao.setLockState(node, LockState.Locked))

        val next = dispatcherService.getNext()
        assertNull(next)
    }

    @Test
    fun testStartAndStopTask() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"))
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                null).apply { endpoint = analyst }

        val node = analystDao.create(aspec)

        val next = dispatcherService.getNext()
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
    fun testStopErrorTask() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Document(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Document(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec("test_job",
                ZpsScript("foo",
                        generate = null,
                        execute = null,
                        over=listOf(doc1, doc2)))
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
                1024,
                648,
                1024,
                0.5f,
                null).apply { endpoint = analyst }

        val next = dispatcherService.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(1)))

            authenticate()
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter(jobIds=listOf(next.jobId))).size())
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter(taskIds=listOf(next.taskId))).size())
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter()).size())
        }
    }

    @Test
    fun testExpand() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobService.create(spec)
        val task = dispatcherService.expand(job, emptyZpsScript("bar"))
        assertEquals(job.id, task.jobId)
    }


    @Test
    fun testExpandFromParentTask() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobService.create(spec)
        val zps = emptyZpsScript("bar")
        zps.execute = mutableListOf(ProcessorRef("foo"))

        val task = dispatcherService.expand(job, zps)
        val task2 = dispatcherService.expand(task, emptyZpsScript("bar"))
        val zps2 = taskDao.getScript(task2.id)

        assertNotNull(zps2.execute)
        // Validate task2 inherited from task
        assertEquals(1, zps.execute!!.size)
    }
}