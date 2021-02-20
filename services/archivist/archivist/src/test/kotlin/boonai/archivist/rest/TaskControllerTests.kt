package boonai.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import boonai.archivist.MockMvcTest
import boonai.archivist.domain.Job
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.JobState
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.domain.Task
import boonai.archivist.domain.TaskError
import boonai.archivist.domain.TaskErrorEvent
import boonai.archivist.domain.TaskEvent
import boonai.archivist.domain.TaskEventType
import boonai.archivist.domain.TaskFilter
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.ZpsScript
import boonai.archivist.domain.emptyZpsScript
import boonai.archivist.domain.emptyZpsScripts
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.TaskErrorDao
import boonai.archivist.service.JobService
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.randomString
import boonai.common.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@WebAppConfiguration
class TaskControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    lateinit var task: Task

    @Before
    fun init() {
        val job = launchJob()
        // create additional task
        task = jobService.createTask(job, emptyZpsScript("bar"))
    }

    fun launchJob(): Job {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        return jobService.create(spec)
    }

    @Test
    fun testGet() {

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/tasks/" + task.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Task::class.java)
        assertEquals(task.id, t1.id)
    }

    @Test
    fun testSearchByJobId() {

        val filter = TaskFilter(jobIds = listOf(task.jobId))
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/tasks/_search")
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val list = deserialize(result, object : TypeReference<KPagedList<Task>>() {})
        assertEquals(2, list.size())
    }

    @Test
    fun testSearchByTaskId() {

        val filter = TaskFilter(ids = listOf(task.id))
        val body = Json.serializeToString(filter)
        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/tasks/_search")
                .headers(admin())
                .content(body)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val list = deserialize(result, object : TypeReference<KPagedList<Task>>() {})
        assertEquals(1, list.size())
    }

    @Test
    fun testFindOne() {
        val result = resultForPostContent<Task>(
            "/api/v1/tasks/_findOne",
            TaskFilter(ids = listOf(task.id))
        )
        assertEquals(task.id, result.id)
        assertEquals(task.name, result.name)
        assertEquals(task.projectId, result.projectId)
    }

    @Test
    fun testRetry() {
        jobService.getTasks(task.jobId).list.forEach {
            assertTrue(jobService.setTaskState(it, TaskState.Failure, null))
        }
        var job = jobService.get(task.jobId)
        assertEquals(JobState.Failure, job.state)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/tasks/${task.id}/_retry")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("retry", status["op"])
        assertEquals(task.id.toString(), status["id"])
        assertEquals("Task", status["type"])
        assertEquals(true, status["success"])

        val ct = jobService.getTask(task.id)
        assertEquals(TaskState.Waiting, ct.state)

        job = jobService.get(task.jobId)
        assertEquals(JobState.InProgress, job.state)
    }

    @Test
    fun testRetryOnRunningTask() {
        jobService.setTaskState(task, TaskState.Running, null)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/tasks/${task.id}/_retry")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("retry", status["op"])
        assertEquals(task.id.toString(), status["id"])
        assertEquals("Task", status["type"])
        assertEquals(true, status["success"])

        // Won't be set to Waiting until task ends
        val ct = jobService.getTask(task.id)
        assertEquals(TaskState.Running, ct.state)
    }

    @Test
    fun testSkip() {

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/tasks/${task.id}/_skip")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("skip", status["op"])
        assertEquals(task.id.toString(), status["id"])
        assertEquals("Task", status["type"])
        assertEquals(true, status["success"])

        val ct = jobService.getTask(task.id)
        assertEquals(TaskState.Skipped, ct.state)
    }

    @Test
    fun testGetScript() {

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/tasks/${task.id}/_script")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val script = deserialize(result, ZpsScript::class.java)
        assertEquals("bar", script.name)
    }

    @Test
    fun testGetLogFile() {

        val locator = task.getLogFileLocation()
        projectStorageService.store(ProjectStorageSpec(locator, mapOf(), "foo".toByteArray()))

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/tasks/${task.id}/_log")
                .headers(admin())
                .contentType(MediaType.TEXT_PLAIN)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val content = result.response.contentAsString
        assertTrue(content.contains("foo"))
    }

    @Test
    fun testGetLogFile204() {

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/tasks/${task.id}/_log")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().`is`(204))
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testGetTaskErrors() {

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
            "it broke", "com.zorroa.OfficeIngestor", true, "execute"
        )
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        taskErrorDao.create(task, error)

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/tasks/${task.id}/taskerrors")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val content = result.response.contentAsString
        val log = Json.Mapper.readValue<KPagedList<TaskError>>(
            content,
            object : TypeReference<KPagedList<TaskError>>() {}
        )
        assertEquals(1, log.size())
    }

    @Test
    fun testResolveDepends() {
        val tasks = emptyZpsScripts("foo")
        tasks[0].children = emptyZpsScripts("bar")
        val spec = JobSpec(
            "test_job2",
            tasks,
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job2 = jobService.create(spec)
        val dependTask = jobService.getTasks(job2.id).list.find { it.state == TaskState.Depend }

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/tasks/${dependTask?.id}/_drop_depends")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.dropped", CoreMatchers.equalTo(1)))
            .andReturn()

        authenticate()
        val updatedTask = jobService.getTask(dependTask!!.id)
        assertEquals(TaskState.Waiting, updatedTask.state)
    }
}
