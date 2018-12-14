package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.Task
import com.zorroa.common.domain.TaskSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskErrorDaoTests : AbstractTest() {

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var jobService: JobService

    @Test
    fun testCreate() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(UUID.randomUUID(), "/foo/bar.jpg",
                "it broke", "com.zorroa.ImageIngestor", true, "execute")
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        val result = taskErrorDao.create(event, error)
        assertEquals(error.message, result.message)
        assertEquals(event.jobId, result.jobId)
        assertEquals(event.taskId, result.taskId)
        assertEquals(error.phase, result.phase)
    }

    @Test
    fun testCreateNoFile() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(null, null,
                "it broke", "com.zorroa.ImageIngestor", true, "execute")
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        val result = taskErrorDao.create(event, error)
        assertEquals(error.message, result.message)
        assertEquals(error.phase, result.phase)
        assertEquals(event.jobId, result.jobId)
        assertEquals(event.taskId, result.taskId)

    }

    fun createTaskErrors() : Task {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(UUID.randomUUID(), "/foo/bar.jpg",
                "it broke", "com.zorroa.ImageIngestor", true, "execute")
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        taskErrorDao.create(event, error)
        authenticate("admin")
        return task
    }

    @Test
    fun testDelete() {
        createTaskErrors()
        var filter = TaskErrorFilter()
        assertEquals(1, taskErrorDao.count(filter))
        assertTrue(taskErrorDao.delete( taskErrorDao.getAll(filter)[0].id))
    }

    @Test
    fun testDeleteByJob() {
        val task = createTaskErrors()
        var filter = TaskErrorFilter()
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals(1, taskErrorDao.deleteAll(task))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByProcessor() {
        createTaskErrors()
        var filter = TaskErrorFilter(processors = listOf("com.zorroa.ImageIngestor"))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals("com.zorroa.ImageIngestor",
                taskErrorDao.getAll(filter)[0].processor)

        filter = TaskErrorFilter(processors=listOf("com.zorroa.BilboBaggins"))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByPath() {
        createTaskErrors()
        var filter = TaskErrorFilter(paths = listOf("/foo/bar.jpg"))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals("/foo/bar.jpg", taskErrorDao.getAll(filter)[0].path)

        filter = TaskErrorFilter(paths=listOf("/foo/xxx/BilboBaggins"))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByTaskAndJob() {
        val task = createTaskErrors()
        var filter = TaskErrorFilter(taskIds = listOf(task.id), jobIds = listOf(task.jobId))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals("/foo/bar.jpg", taskErrorDao.getAll(filter)[0].path)

        filter = TaskErrorFilter(taskIds=listOf(UUID.randomUUID()))
        assertEquals(0, taskErrorDao.count(filter))

        filter = TaskErrorFilter(taskIds=listOf(UUID.randomUUID()), jobIds= listOf())
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAllByAssetId() {
        val task = createTaskErrors()
        val assetId = UUID.randomUUID()
        jdbc.update("UPDATE task_error SET pk_asset=?", assetId)

        var filter = TaskErrorFilter(assetIds=listOf(assetId))
        assertEquals(1, taskErrorDao.count(filter))
        assertEquals(assetId, taskErrorDao.getAll(filter)[0].assetId)

        filter = TaskErrorFilter(assetIds=listOf(UUID.randomUUID()))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testGetAlByTime() {
        createTaskErrors()

        var filter = TaskErrorFilter(timeCreated = LongRangeFilter(0, System.currentTimeMillis()+1000))
        assertEquals(1, taskErrorDao.count(filter))

        filter = TaskErrorFilter(timeCreated = LongRangeFilter(System.currentTimeMillis()+1000, null))
        assertEquals(0, taskErrorDao.count(filter))
    }

    @Test
    fun testSort() {
        // Add a bunch of tasks
        val task = createTaskErrors()
        authenticateAsAnalyst()
        for (i in 0 .. 10) {
            val num = Random().nextInt(1000 )
            val error = TaskErrorEvent(UUID.randomUUID(), String.format("%04d", num),
                    "it broke", "foo", true, "teardown")
            val event = TaskEvent(TaskEventType.ERROR, task.id, task.jobId, error)
            taskErrorDao.create(event, error)
        }

        // Fetch them sorted
        authenticate("admin")
        var filter = TaskErrorFilter(processors=listOf("foo"))
        filter.sort = listOf("path:d")

        var lastNum = 1001
        for (p in taskErrorDao.getAll(filter)) {
            val number : Int = p.path!!.toInt()
            println("$number <= $lastNum")
            assertTrue(number <= lastNum)
            lastNum = number
        }
    }
}