package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Analyst
import com.zorroa.archivist.domain.AnalystSpec
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.StackTraceElement
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.TaskExpandEvent
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.TaskStoppedEvent
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.service.DispatchQueueManager
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import com.zorroa.zmlp.util.Json
import com.zorroa.archivist.util.randomString
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebAppConfiguration
class AnalystClusterControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var analystService: AnalystService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var dispatchQueueManager: DispatchQueueManager

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

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
    fun testStartedEvent() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {
            val te = TaskEvent(
                TaskEventType.STARTED,
                task.id,
                job.id,
                emptyMap<String, String>()
            )

            mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(TaskState.Running, rtask.state)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testUpdateEventProgress() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {
            val te = TaskEvent(
                TaskEventType.PROGRESS,
                task.id,
                job.id,
                mapOf("progress" to 100)
            )

            val andReturn = mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(100, rtask.progress)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testUpdateEventStatus() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {
            val te = TaskEvent(
                TaskEventType.STATUS,
                task.id,
                job.id,
                mapOf("status" to "testingStatus")
            )

            val andReturn = mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals("testingStatus", rtask.status)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testStoppedEventSuccess() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {
            assertTrue(dispatcherService.startTask(task))
            val te = TaskEvent(
                TaskEventType.STOPPED,
                task.id,
                job.id,
                TaskStoppedEvent(0, null)
            )

            mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(TaskState.Success, rtask.state)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testStoppedEventTaskFailed() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {
            assertTrue(dispatcherService.startTask(task))
            jdbc.update("UPDATE task SET int_run_count=100 WHERE pk_task=?", task.id)
            val te = TaskEvent(
                TaskEventType.STOPPED,
                task.id,
                job.id,
                TaskStoppedEvent(1, null)
            )

            mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(TaskState.Failure, rtask.state)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testExpandEvent() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {

            assertTrue(dispatcherService.startTask(task))
            val te = TaskEvent(
                TaskEventType.EXPAND,
                task.id,
                job.id,
                TaskExpandEvent(listOf(AssetSpec("http://foo/bar/dog.jpg")))
            )

            mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            val count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM task WHERE pk_job=?",
                Int::class.java, task.jobId
            )
            assertEquals(2, count)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testErrorEvent() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {

            assertTrue(dispatcherService.startTask(task))
            val tev = TaskErrorEvent(
                randomString(), "/foo/bar.jpg", "it broke",
                "com.zorroa.ImageIngestor", true, "execute"
            )
            val te = TaskEvent(
                TaskEventType.ERROR,
                task.id,
                job.id,
                tev
            )

            mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            authenticate()
            val terr = taskErrorDao.getLast()
            assertEquals(task.id, terr.taskId)
            assertEquals(task.jobId, terr.jobId)
            assertEquals(true, terr.fatal)
            assertEquals(tev.path, terr.path)
            assertEquals(tev.message, terr.message)
            assertEquals(tev.processor, terr.processor)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testErrorEventWithStackTraceElements() {
        val job = launchJob()
        authenticateAsAnalyst()
        val task = dispatchQueueManager.getNext()

        if (task != null) {

            assertTrue(dispatcherService.startTask(task))
            val tev = TaskErrorEvent(
                randomString(), "/foo/bar.jpg", "it broke",
                "com.zorroa.ImageIngestor", true, "execute", stackTrace = listOf(StackTraceElement())
            )

            val te = TaskEvent(
                TaskEventType.ERROR,
                task.id,
                job.id,
                tev
            )

            mvc.perform(
                MockMvcRequestBuilders.post("/cluster/_event")
                    .headers(analyst())
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te))
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            authenticate()
            val terr = taskErrorDao.getLast()
            val stack = terr.stackTrace?.get(0)
            assertNotNull(stack)
            assertEquals(stack.file, "Unknown File")
            assertEquals(stack.className, "Unknown Class")
            assertEquals(stack.methodName, "Unknown Method")
            assertEquals(stack.lineNumber, 0)
        } else {
            assertNotNull(task)
        }
    }

    @Test
    fun testPing() {
        authenticateAsAnalyst()

        val spec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.41.0",
            null
        )

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/cluster/_ping")
                .headers(analyst())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val analyst = Json.Mapper.readValue<Analyst>(result.response.contentAsString, Analyst::class.java)
        assertTrue(analystService.exists(analyst.endpoint))
    }

    @Test
    fun testQueue() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec)

        authenticateAsAnalyst()
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.41.0",
            null
        )

        analystService.upsert(aspec)

        mvc.perform(
            MockMvcRequestBuilders.put("/cluster/_queue")
                .headers(analyst())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        mvc.perform(
            MockMvcRequestBuilders.put("/cluster/_queue")
                .headers(analyst())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }
}
