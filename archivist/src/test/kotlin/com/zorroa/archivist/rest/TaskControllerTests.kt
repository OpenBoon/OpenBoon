package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.domain.TaskError
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.Task
import com.zorroa.common.domain.TaskFilter
import com.zorroa.common.domain.TaskSpec
import com.zorroa.common.domain.TaskState
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.nio.file.Files
import java.util.UUID
import kotlin.test.assertEquals

@WebAppConfiguration
class TaskControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var fileStorageService: FileStorageService

    lateinit var task: Task

    @Before
    fun init() {
        val job = launchJob()
        // create additional task
        task = jobService.createTask(job, TaskSpec("bar", emptyZpsScript("bar")))
    }

    fun launchJob(): Job {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
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
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
        assertEquals(task.organizationId, result.organizationId)
    }

    @Test
    fun testRetry() {
        jobService.setTaskState(task, TaskState.Failure, null)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/tasks/${task.id}/_retry")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
    }

    @Test
    fun testRetryOnRunningTask() {
        jobService.setTaskState(task, TaskState.Running, null)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/tasks/${task.id}/_retry")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
                .with(SecurityMockMvcRequestPostProcessors.csrf())
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
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val script = deserialize(result, ZpsScript::class.java)
        assertEquals("bar", script.name)
    }

    @Test
    fun testGetLogFile() {

        val log = task.getLogSpec()
        val fs = fileStorageService.get(log)
        fileStorageService.getSignedUrl(fs.id, HttpMethod.PUT)

        Files.write(fs.getServableFile().getLocalFile(), "boom!".toByteArray())

        val req = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/tasks/${task.id}/_log")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val data = req.response.contentAsString
        assertEquals("boom!", data)
    }

    @Test
    fun testGetLogFile404() {

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/tasks/${task.id}/_log")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testGetTaskErrors() {

        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            UUID.randomUUID(), "/foo/bar.jpg",
            "it broke", "com.zorroa.OfficeIngestor", true, "execute"
        )
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        taskErrorDao.create(task, error)
        authenticate("admin")

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/tasks/${task.id}/taskerrors")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val content = result.response.contentAsString
        val log = Json.Mapper.readValue<KPagedList<TaskError>>(content,
            object : TypeReference<KPagedList<TaskError>>() {})
        assertEquals(1, log.size())
    }
}
