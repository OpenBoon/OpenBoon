package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobId
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobType
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskFilter
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.emptyZpsScript
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

    @Autowired
    internal lateinit var taskDao: TaskDao

    internal lateinit var task: Task
    internal lateinit var spec: TaskSpec
    internal lateinit var job: Job

    @Before
    fun init() {
        val jspec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        job = jobDao.create(jspec, JobType.Import)
        spec = TaskSpec("generator", jspec.script!!)
        task = taskDao.create(job, spec)
    }

    @Test
    fun getPagedByFilter() {
        for (i in 0..9) {
            taskDao.create(job, TaskSpec("test$i", emptyZpsScript("test_script")))
        }

        val filter1 = TaskFilter(jobIds = listOf(job.id))
        filter1.page = KPage(0, 10)
        assertEquals(10, taskDao.getAll(filter1).size())

        val filter2 = TaskFilter(jobIds = listOf(UUID.randomUUID()))
        assertEquals(0, taskDao.getAll(filter2).size())

        val filter3 = TaskFilter(jobIds = listOf(job.id))
        assertEquals(11, taskDao.getAll(filter3).size())

        val filter4 = TaskFilter(states = listOf(TaskState.Skipped))
        assertEquals(0, taskDao.getAll(filter4).size())
    }

    @Test
    fun testGetScript() {
        val script = taskDao.getScript(task.id)
        assertEquals("test_script", script.name)
    }

    @Test
    fun testGet() {
        val task2 = taskDao.get(task.id)
        assertEquals(task.id, task2.id)
        assertEquals(job.projectId, task2.projectId)
    }

    @Test
    fun testGetInternal() {
        val itask = taskDao.getInternal(task.id)
        assertEquals(task.id, itask.taskId)
        assertEquals(task.jobId, itask.jobId)
        assertEquals(task.name, itask.name)
        assertEquals(task.state, itask.state)
    }

    @Test
    fun testGetHostEndpoint() {
        val url = "https://foo.bar:1234"
        var endpoint = taskDao.getHostEndpoint(task)
        assertNull(endpoint)

        jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", url, task.id)
        endpoint = taskDao.getHostEndpoint(task)
        assertEquals(url, endpoint)
    }

    @Test
    fun testCreate() {
        assertEquals(job.id, task.jobId)
        assertEquals(spec.name, task.name)
    }

    @Test
    fun testSetState() {
        assertEquals(1, taskStateCount(job, TaskState.Waiting))
        assertTrue(taskDao.setState(task, TaskState.Running, null))
        assertEquals(0, taskStateCount(job, TaskState.Waiting))
        assertEquals(1, taskStateCount(job, TaskState.Running))
        assertFalse(taskDao.setState(task, TaskState.Running, TaskState.Waiting))
        assertFalse(taskDao.setState(task, TaskState.Skipped, TaskState.Waiting))
    }

    @Test
    fun testCheckStartTime() {
        assertEquals(1, taskStateCount(job, TaskState.Waiting))
        assertTrue(taskDao.setState(task, TaskState.Running, null))
        var task = taskDao.get(task.id)
        assertTrue(task.timeStarted > 0)
        assertTrue(taskDao.setState(task, TaskState.Waiting, null))
        task = taskDao.get(task.id)
        assertEquals(task.timeStarted, -1L)
        assertEquals(task.timeStopped, -1L)
    }

    @Test(expected = DataIntegrityViolationException::class)
    fun testSetStateMaxRunningFailure() {
        jdbc.update("UPDATE job_count SET int_max_running_tasks=0")
        taskDao.setState(task, TaskState.Running, null)
    }

    fun taskStateCount(job: JobId, state: TaskState): Int {
        val ord = state.ordinal
        return jdbc.queryForObject("SELECT int_task_state_$ord FROM job_count WHERE pk_job=?", Int::class.java, job.jobId)
    }

    @Test
    fun testIncrementAssetCounters() {
        val counters = AssetCounters(
                replaced = 4,
                created = 6)

        assertTrue(taskDao.incrementAssetCounters(task, counters))

        val map = jdbc.queryForMap("SELECT * FROM task_stat WHERE pk_task=?", task.id)
        assertEquals(counters.created, map["int_asset_create_count"])
        assertEquals(counters.replaced, map["int_asset_replace_count"])
    }

    @Test
    fun testIsAutoRetryable() {
        assertTrue(taskDao.isAutoRetryable(task))
        jdbc.update("UPDATE task SET int_run_count=4 WHERE pk_task=?", task.taskId)
        assertFalse(taskDao.isAutoRetryable(task))
    }

    @Test
    fun uptimePingTime() {
        val endpoint = "http://localhost:5000"
        assertFalse(taskDao.updatePingTime(task.id, endpoint))
        jdbc.update("UPDATE task SET int_state=?, str_host=?",
                TaskState.Running.ordinal, endpoint)
        assertTrue(taskDao.updatePingTime(task.id, endpoint))
    }

    @Test
    fun testGetOrphans() {
        assertTrue(taskDao.getOrphans(Duration.ofMinutes(1)).isEmpty())

        val endpoint = "http://localhost:5000"
        val time = System.currentTimeMillis() - (86400 * 1000)
        assertFalse(taskDao.updatePingTime(task.id, endpoint))
        jdbc.update("UPDATE task SET time_ping=?, int_state=?, str_host=?",
                time, TaskState.Running.ordinal, endpoint)
        assertTrue(taskDao.getOrphans(Duration.ofMinutes(1)).isNotEmpty())
    }
}
