package com.zorroa.archivist.web

import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.*
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    lateinit var job: Job

    @Before
    fun init() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        job = jobService.create(spec)
    }

    @Test
    fun testGet() {
        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.get("/api/v1/jobs/" + job.id)
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(job.id, t1.id)
    }

    @Test
    fun testCreate() {
        val spec = JobSpec("test_job_2",
                emptyZpsScript("test"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.post("/api/v1/jobs")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(spec.name, t1.name)
        assertEquals(1, t1.taskCounts!!["tasksTotal"])
        assertEquals(1, t1.taskCounts!!["tasksWaiting"])
    }

    @Test
    fun testUpdate() {
        val spec = JobUpdateSpec(name="silly_bazilly", priority = 5)

        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(spec.name, t1.name)
        assertEquals(spec.priority, t1.priority)
    }

    @Test
    fun testCancel() {
        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_cancel")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val job = jobService.get(job.id)
        assertEquals(JobState.Cancelled, job.state)

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("cancel", status["op"])
        assertEquals(true, status["success"])
    }

    @Test
    fun testRestart() {
        jobService.setJobState(job, JobState.Cancelled, null)
        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_restart")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val job = jobService.get(job.id)
        assertEquals(JobState.Active, job.state)

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("restart", status["op"])
        assertEquals(true, status["success"])
    }

    @Test
    fun testRetryAllFailures() {
        val t = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))
        assertTrue(jobService.setTaskState(t, TaskState.Failure, null))

        val session = admin()
        val result = mvc.perform(MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_retryAllFailures")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("retryAllFailures", status["op"])
        assertEquals(true, status["success"])

        val t2 = jobService.getTask(t.id)
        assertEquals(TaskState.Waiting, t2.state)
    }
}