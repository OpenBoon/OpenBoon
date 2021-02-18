package boonai.archivist.repository

import boonai.archivist.AbstractTest
import boonai.archivist.domain.JobId
import boonai.archivist.domain.StackTraceElement
import boonai.archivist.domain.TaskErrorEvent
import boonai.archivist.domain.TaskErrorFilter
import boonai.archivist.domain.TaskEvent
import boonai.archivist.domain.TaskEventType
import boonai.archivist.domain.emptyZpsScript
import boonai.archivist.service.JobService
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.Task
import boonai.archivist.domain.TaskId
import boonai.archivist.domain.emptyZpsScripts
import boonai.archivist.util.LongRangeFilter
import boonai.archivist.util.randomString
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Random
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskErrorDaoTests : AbstractTest() {

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var jobService: JobService

    @Test
    fun testCreate() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, emptyZpsScript("bar"))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            randomString(), "/foo/bar.jpg",
            "it broke", "com.zorroa.ImageIngestor", true, "execute",
            listOf(StackTraceElement("foo.py", 100, "Bar", "jimbob()"))
        )
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        val result = taskErrorDao.create(task, error)
        assertEquals(error.message, result.message)
        assertEquals(event.jobId, result.jobId)
        assertEquals(event.taskId, result.taskId)
        assertEquals(error.phase, result.phase)
        assertEquals(error.stackTrace, result.stackTrace)
    }

    @Test
    fun testCreateWithNullStackTrace() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, emptyZpsScript("bar"))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            randomString(), "/foo/bar.jpg",
            "it broke", "com.zorroa.ImageIngestor", true, "execute"
        )

        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        val result = taskErrorDao.create(task, error)
        assertEquals(error.message, result.message)
        assertEquals(event.jobId, result.jobId)
        assertEquals(event.taskId, result.taskId)
        assertEquals(error.phase, result.phase)
        assertNull(result.stackTrace)
    }

    @Test
    fun testBatchCreate() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, emptyZpsScript("bar"))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            randomString(), "/foo/bar.jpg",
            "it broke", "com.zorroa.ImageIngestor", true, "execute",
            listOf(StackTraceElement("foo.py", 100, "Bar", "jimbob()"))
        )
        val result = taskErrorDao.batchCreate(task, listOf(error, error, error))
        assertEquals(3, result)
    }

    @Test
    fun testCreateNoFile() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, emptyZpsScript("bar"))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            null, null,
            "it broke", "com.zorroa.ImageIngestor", true, "execute"
        )
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        val result = taskErrorDao.create(task, error)
        assertEquals(error.message, result.message)
        assertEquals(error.phase, result.phase)
        assertEquals(event.jobId, result.jobId)
        assertEquals(event.taskId, result.taskId)
    }

    fun createTaskErrors(): Task {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, emptyZpsScript("bar"))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            randomString(), "/foo/bar.jpg",
            "it broke", "com.zorroa.ImageIngestor", true, "execute",
            listOf(StackTraceElement("foo.py", 100, "Bar", "jimbob()"))
        )
        taskErrorDao.create(task, error)
        authenticate()
        return task
    }

    @Test
    fun testGet() {
        createTaskErrors()
        var filter = TaskErrorFilter()
        val id = taskErrorDao.getAll(filter)[0].id
        val error = taskErrorDao.get(id)
        assertEquals(id, error.id)
        assertNotNull(error.stackTrace)
    }

    @Test
    fun testDelete() {
        createTaskErrors()
        var filter = TaskErrorFilter()
        assertEquals(1, taskErrorDao.count(filter))
        assertTrue(taskErrorDao.delete(taskErrorDao.getAll(filter)[0].id))
    }

    @Test
    fun testDeleteByJob() {
        val task = createTaskErrors()
        var filter = TaskErrorFilter()
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM job_stat WHERE pk_job=?",
                Int::class.java, task.jobId
            )
        )
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM task_stat WHERE pk_task=?",
                Int::class.java, task.id
            )
        )
        assertEquals(1, taskErrorDao.deleteAll(task as JobId))
        assertEquals(0, taskErrorDao.count(filter))
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM job_stat WHERE pk_job=?",
                Int::class.java, task.jobId
            )
        )
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM task_stat WHERE pk_task=?",
                Int::class.java, task.id
            )
        )
    }

    @Test
    fun testDeleteByTask() {
        val task = createTaskErrors()
        var filter = TaskErrorFilter()
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM job_stat WHERE pk_job=?",
                Int::class.java, task.jobId
            )
        )
        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM task_stat WHERE pk_task=?",
                Int::class.java, task.id
            )
        )
        assertEquals(1, taskErrorDao.deleteAll(task as TaskId))
        assertEquals(0, taskErrorDao.count(filter))
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM job_stat WHERE pk_job=?",
                Int::class.java, task.jobId
            )
        )
        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT int_asset_error_count FROM task_stat WHERE pk_task=?",
                Int::class.java, task.id
            )
        )
    }

    @Test
    fun testGetAllByProcessor() {
        createTaskErrors()
        var filter = TaskErrorFilter(processors = listOf("com.zorroa.ImageIngestor"))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals(
            "com.zorroa.ImageIngestor",
            taskErrorDao.getAll(filter)[0].processor
        )

        filter = TaskErrorFilter(processors = listOf("com.zorroa.BilboBaggins"))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByPath() {
        createTaskErrors()
        var filter = TaskErrorFilter(paths = listOf("/foo/bar.jpg"))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals("/foo/bar.jpg", taskErrorDao.getAll(filter)[0].path)

        filter = TaskErrorFilter(paths = listOf("/foo/xxx/BilboBaggins"))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByTaskAndJob() {
        val task = createTaskErrors()
        var filter = TaskErrorFilter(taskIds = listOf(task.id), jobIds = listOf(task.jobId))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals("/foo/bar.jpg", taskErrorDao.getAll(filter)[0].path)

        filter = TaskErrorFilter(taskIds = listOf(UUID.randomUUID()))
        assertEquals(0, taskErrorDao.count(filter))

        filter = TaskErrorFilter(taskIds = listOf(UUID.randomUUID()), jobIds = listOf())
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByAssetId() {
        val task = createTaskErrors()
        val assetId = randomString()
        jdbc.update("UPDATE task_error SET asset_id=?", assetId)

        var filter = TaskErrorFilter(assetIds = listOf(assetId))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals(assetId, taskErrorDao.getAll(filter)[0].assetId)

        filter = TaskErrorFilter(assetIds = listOf(randomString()))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAlByTime() {
        createTaskErrors()

        var filter = TaskErrorFilter(timeCreated = LongRangeFilter(0, System.currentTimeMillis() + 1000))
        assertEquals(1, taskErrorDao.count(filter))

        filter = TaskErrorFilter(timeCreated = LongRangeFilter(System.currentTimeMillis() + 1000, null))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByKeywords() {
        createTaskErrors()

        var filter = TaskErrorFilter(keywords = "foo & bar")
        assertEquals(1, taskErrorDao.count(filter))

        filter = TaskErrorFilter(keywords = "foo & cat")
        assertEquals(0, taskErrorDao.count(filter))

        filter = TaskErrorFilter(keywords = "bar.jpg")
        assertEquals(1, taskErrorDao.count(filter))

        filter = TaskErrorFilter(keywords = "/foo/bar.jpg")
        assertEquals(1, taskErrorDao.count(filter))
    }

    @Test
    fun testSort() {
        // Add a bunch of tasks
        val task = createTaskErrors()
        authenticateAsAnalyst()
        for (i in 0..10) {
            val num = Random().nextInt(1000)
            val error = TaskErrorEvent(
                randomString(), String.format("%04d", num),
                "it broke", "foo", true, "teardown"
            )
            val event = TaskEvent(TaskEventType.ERROR, task.id, task.jobId, error)
            taskErrorDao.create(task, error)
        }
        authenticate()
        var filter = TaskErrorFilter(processors = listOf("foo"))
        filter.sort = listOf("path:d")

        var lastNum = 1001
        for (p in taskErrorDao.getAll(filter)) {
            val number: Int = p.path!!.toInt()
            println("$number <= $lastNum")
            assertTrue(number <= lastNum)
            lastNum = number
        }
    }
}
