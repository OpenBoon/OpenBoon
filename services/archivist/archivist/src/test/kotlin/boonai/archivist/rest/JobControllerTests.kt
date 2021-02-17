package boonai.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import boonai.archivist.MockMvcTest
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsType
import boonai.archivist.domain.TaskError
import boonai.archivist.domain.TaskErrorEvent
import boonai.archivist.domain.TaskEvent
import boonai.archivist.domain.TaskEventType
import boonai.archivist.domain.emptyZpsScript
import boonai.archivist.repository.TaskErrorDao
import boonai.archivist.service.JobService
import boonai.archivist.domain.Job
import boonai.archivist.domain.JobFilter
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.JobState
import boonai.archivist.domain.JobUpdateSpec
import boonai.archivist.domain.TaskState
import boonai.archivist.domain.emptyZpsScripts
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.CredentialsService
import boonai.common.util.Json
import boonai.archivist.util.randomString
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    lateinit var job: Job

    @Before
    fun init() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        job = jobService.create(spec)
    }

    @Test
    fun testGet() {

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/jobs/" + job.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(job.id, t1.id)
    }

    @Test
    fun testCreate() {
        val spec = JobSpec(
            "test_job_2",
            emptyZpsScripts("test"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/jobs")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(spec.name, t1.name)
        assertEquals(1, t1.taskCounts!!["tasksTotal"])
        assertEquals(1, t1.taskCounts!!["tasksWaiting"])
    }

    @Test
    fun testUpdate() {
        val spec = JobUpdateSpec("silly_bazilly", 5, true, System.currentTimeMillis(), 5)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(spec.name, t1.name)
        assertEquals(spec.priority, t1.priority)
        assertEquals(spec.maxRunningTasks, t1.maxRunningTasks)
    }

    @Test
    fun testCancel() {

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_cancel")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
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

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_restart")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val job = jobService.get(job.id)
        assertEquals(JobState.InProgress, job.state)

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("restart", status["op"])
        assertEquals(true, status["success"])
    }

    @Test
    fun testRetryAllFailures() {
        val t = jobService.createTask(job, emptyZpsScript("bar"))
        jobService.getTasks(job.id).list.forEach {
            assertTrue(jobService.setTaskState(it, TaskState.Failure, null))
        }

        var job = jobService.get(t.jobId)
        assertEquals(JobState.Failure, job.state)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_retryAllFailures")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("retryAllFailures", status["op"])
        assertEquals(true, status["success"])

        val t2 = jobService.getTask(t.id)
        assertEquals(TaskState.Waiting, t2.state)
        job = jobService.get(t.jobId)
        assertEquals(JobState.InProgress, job.state)
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
            MockMvcRequestBuilders.post("/api/v1/jobs/${job.id}/taskerrors")
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
    @Throws(Exception::class)
    fun testSearch() {
        val jobs = resultForPostContent<KPagedList<Job>>(
            "/api/v1/jobs/_search",
            JobFilter()
        )
        assertTrue(jobs.size() > 0)
    }

    @Test
    fun testFindOneWithEmptyFilter() {
        val job = resultForPostContent<Job>(
            "/api/v1/jobs/_findOne",
            JobFilter()
        )
        assertEquals("test_job", job.name)
    }

    @Test
    fun testFindOneWithFilter() {
        val spec = jobSpec("baz")
        jobService.create(spec)
        val job = resultForPostContent<Job>(
            "/api/v1/jobs/_findOne",
            JobFilter(names = listOf("baz_job"))
        )
        assertEquals(spec.name, job.name)
    }

    private fun jobSpec(name: String): JobSpec {
        return JobSpec(
            "${name}_job",
            emptyZpsScripts("${name}_script"),
            args = mutableMapOf("${name}_arg" to 1),
            env = mutableMapOf("${name}_env_var" to "${name}_env_value")
        )
    }

    @Test
    fun testGetDescryptedCredentials() {
        credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )

        val spec2 = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar"),
            credentials = setOf("test")
        )
        val job2 = jobService.create(spec2)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/jobs/${job2.id}/_credentials/AWS")
                .headers(job())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.aws_access_key_id",
                    CoreMatchers.equalTo("foo")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.aws_secret_access_key",
                    CoreMatchers.equalTo("kirkspockbones")
                )
            )
            .andReturn()
    }

    @Test
    fun testDropDepends() {

        authenticate()
        val spec = JobSpec(
            "test_job_3",
            emptyZpsScripts("test"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job1 = jobService.create(spec)
        spec.dependOnJobIds = listOf(job1.id)
        val job2 = jobService.create(spec)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/jobs/${job2.id}/_drop_depends")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.dropped", CoreMatchers.equalTo(1)))
    }
}
