package com.zorroa.archivist.web

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.*
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
    lateinit var taskErrorDao: TaskErrorDao

    fun launchJob() : Job {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        return jobService.create(spec)
    }

    @Test
    fun testStartedEvent() {
        val job = launchJob()
        val task = dispatcherService.getNext("http://localhost:1234")

        if (task != null) {
            val te = TaskEvent("started",
                    "http://localhost:1234",
                    task.id,
                    job.id,
                    emptyMap<String,String>())

            SecurityContextHolder.getContext().authentication = null
            mvc.perform(MockMvcRequestBuilders.post("/cluster/_event")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te)))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(TaskState.Running, rtask.state)
        }
        else {
            assertNotNull(task)
        }
    }

    @Test
    fun testStoppedEventSuccess() {
        val job = launchJob()
        val task = dispatcherService.getNext("http://localhost:1234")

        if (task != null) {
            assertTrue(dispatcherService.startTask(task))
            val te = TaskEvent("stopped",
                    "http://localhost:1234",
                    task.id,
                    job.id,
                    TaskStoppedEvent(0))

            SecurityContextHolder.getContext().authentication = null
            mvc.perform(MockMvcRequestBuilders.post("/cluster/_event")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te)))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(TaskState.Success, rtask.state)
        }
        else {
            assertNotNull(task)
        }
    }

    @Test
    fun testStoppedEventFailure() {
        val job = launchJob()
        val task = dispatcherService.getNext("http://localhost:1234")

        if (task != null) {
            assertTrue(dispatcherService.startTask(task))
            val te = TaskEvent("stopped",
                    "http://localhost:1234",
                    task.id,
                    job.id,
                    TaskStoppedEvent(1))

            SecurityContextHolder.getContext().authentication = null
            mvc.perform(MockMvcRequestBuilders.post("/cluster/_event")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te)))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val rtask = jobService.getTask(task.id)
            assertEquals(TaskState.Fail, rtask.state)
        }
        else {
            assertNotNull(task)
        }
    }

    @Test
    fun testExpandEvent() {
        val job = launchJob()
        val task = dispatcherService.getNext("http://localhost:1234")

        if (task != null) {

            assertTrue(dispatcherService.startTask(task))
            val te = TaskEvent("expand",
                    "http://localhost:1234",
                    task.id,
                    job.id,
                    emptyZpsScript("bob"))

            SecurityContextHolder.getContext().authentication = null
            mvc.perform(MockMvcRequestBuilders.post("/cluster/_event")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te)))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val count = jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE pk_job=?",
                    Int::class.java, task.jobId)
            assertEquals(2, count)
        }
        else {
            assertNotNull(task)
        }
    }

    @Test
    fun testErrorEvent() {
        val job = launchJob()
        val task = dispatcherService.getNext("http://localhost:1234")

        if (task != null) {

            assertTrue(dispatcherService.startTask(task))
            val tev = TaskErrorEvent(UUID.randomUUID(),"/foo/bar.jpg","it broke",
                    "com.zorroa.ImageIngestor", true)
            val te = TaskEvent("error",
                    "http://localhost:1234",
                    task.id,
                    job.id,
                    tev)

            SecurityContextHolder.getContext().authentication = null
            mvc.perform(MockMvcRequestBuilders.post("/cluster/_event")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content(Json.serialize(te)))
                    .andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()

            val terr = taskErrorDao.getLast()
            assertEquals(task.id, terr.taskId)
            assertEquals(task.jobId, terr.jobId)
            assertEquals(te.endpoint, terr.endpoint)
            assertEquals(true, terr.fatal)
            assertEquals(tev.path, terr.path)
            assertEquals(tev.message, terr.message)
            assertEquals(tev.processor, terr.processor)
        }
        else {
            assertNotNull(task)
        }
    }

    @Test
    fun testPing() {
        SecurityContextHolder.getContext().authentication = null

        val spec = AnalystSpec(
                "http://localhost:1234",
                1024,
                648,
                0.5f,
                null)

        val result = mvc.perform(MockMvcRequestBuilders.post("/cluster/_ping")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val analyst = Json.Mapper.readValue<Analyst>(result.response.contentAsString, Analyst::class.java)
        assertEquals(spec.endpoint, analyst.endpoint)
        assertTrue(analystService.exists(spec.endpoint))

    }

    @Test
    fun testQueue() {

        val aspec = AnalystSpec(
                "https://10.0.0.1",
                1024,
                648,
                0.5f,
                null)

        analystService.upsert(aspec)

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        jobService.create(spec)

        mvc.perform(MockMvcRequestBuilders.put("/cluster/_queue")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("endpoint" to  "https://10.0.0.1"))))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        // This should be 404
        mvc.perform(MockMvcRequestBuilders.put("/cluster/_queue")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("endpoint" to  "https://10.0.0.1"))))
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()
    }

}