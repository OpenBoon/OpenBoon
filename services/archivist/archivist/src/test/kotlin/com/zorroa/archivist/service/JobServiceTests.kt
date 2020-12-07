package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.domain.Task
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.domain.emptyZpsScripts
import com.zorroa.archivist.storage.ProjectStorageException
import com.zorroa.archivist.storage.ProjectStorageService
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    lateinit var spec: JobSpec
    lateinit var job: Job
    lateinit var task: Task

    @Before
    fun init() {
        spec = JobSpec(
            "test_job",
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        job = jobService.create(spec)
        task = jobService.createTask(job, emptyZpsScript("bar"))
    }

    @Test
    fun testCreate() {
        assertEquals(spec.name, job.name)
        val tcount = jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE pk_job=?", Int::class.java, job.id)
        assertEquals(2, tcount)
        jobService.getZpsScript(task.id)
    }

    @Test
    fun testCreateWithJobDepends() {
        val spec2 = JobSpec(
            null,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar"),
            dependOnJobIds = listOf(job.id)
        )
        val job2 = jobService.create(spec2)
        val tasks = jobService.getTasks(job2.id)
        assertEquals(TaskState.Depend, tasks[0].state)
    }

    @Test
    fun testCreateImport() {
        val spec2 = JobSpec(
            null,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job2 = jobService.create(spec2)
        assertEquals(JobPriority.Standard, job2.priority)
        val tasks = jobService.getTasks(job2.id)
        assertEquals(1, tasks.count())
    }

    @Test
    fun testCreateMultipleTasks() {
        val spec2 = JobSpec(
            null,
            listOf(emptyZpsScript("foo"), emptyZpsScript("bar")),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job2 = jobService.create(spec2)
        val tasks = jobService.getTasks(job2.id)
        assertEquals(2, tasks.count())
    }

    @Test
    fun testCreateMultipleTasksWithDepends() {

        val tspec = listOf(
            emptyZpsScript("foo"),
            emptyZpsScript("bar")
        )
        tspec[0].children = listOf(emptyZpsScript("foo1"))
        tspec[1].children = listOf(emptyZpsScript("bar"))

        val spec2 = JobSpec(
            null,
            tspec,
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job2 = jobService.create(spec2)
        val tasks = jobService.getTasks(job2.id)
        assertEquals(4, tasks.count())
    }

    @Test
    fun testCreateWithAutoName() {
        val spec2 = JobSpec(
            null,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job2 = jobService.create(spec2)
        assertTrue("launched" in job2.name)
    }

    @Test
    fun testReplace() {
        val name = "bilbo_baggins_v1"
        val spec1 = JobSpec(
            name,
            emptyZpsScripts("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job1 = jobService.create(spec1)
        assertEquals(spec1.name, job1.name)

        val spec2 = JobSpec(
            name,
            emptyZpsScripts("foo"),
            replace = true
        )
        val job2 = jobService.create(spec2)
        assertEquals(job1.name, job2.name)

        // Should only have 1 job.
        assertEquals(
            1,
            jobService.getAll(
                JobFilter(
                    states = listOf(JobState.InProgress),
                    names = listOf(job1.name)
                )
            ).size()
        )
    }

    @Test
    fun testIncrementAssetCounts() {

        val counters = AssetCounters(
            total = 10
        )

        jobService.incrementAssetCounters(task, counters)

        val map = jdbc.queryForMap("SELECT * FROM task_stat WHERE pk_task=?", task.id)
        assertEquals(counters.total, map["int_asset_total_count"])

        val map2 = jdbc.queryForMap("SELECT * FROM job_stat WHERE pk_job=?", task.jobId)
        assertEquals(counters.total, map2["int_asset_total_count"])
    }

    @Test
    fun checkAndSetJobFinishedSuccess() {
        assertFalse(jobService.checkAndSetJobFinished(job))
        jdbc.update("UPDATE job_count SET int_task_state_0=0, int_task_state_4=2")
        assertTrue(jobService.checkAndSetJobFinished(job))
        val job2 = jobService.get(job.jobId)
        assertEquals(JobState.Success, job2.state)
    }

    @Test
    fun checkAndSetJobFinishedFailed() {
        assertFalse(jobService.checkAndSetJobFinished(job))
        jdbc.update("UPDATE job_count SET int_task_state_0=0, int_task_state_3=2")
        assertTrue(jobService.checkAndSetJobFinished(job))
        val job2 = jobService.get(job.jobId)
        assertEquals(JobState.Failure, job2.state)
    }

    @Test
    fun retryAllTaskFailures() {
        jobService.setTaskState(task, TaskState.Failure, null)
        var updatedJob = jobService.get(job.id)
        val count = jobService.retryAllTaskFailures(updatedJob)
        assertEquals(1, count)
    }

    @Test
    fun testSetCredentials() {
        val creds = credentialsService.create(
            CredentialsSpec(
                "test",
                CredentialsType.AWS, TEST_AWS_CREDS
            )
        )

        val spec2 = JobSpec(
            "test_job2",
            emptyZpsScripts("foo"),
            credentials = setOf(creds.id.toString())
        )
        val job2 = jobService.create(spec2)

        assertEquals(
            1,
            jdbc.queryForObject(
                "SELECT COUNT(1) FROM x_credentials_job WHERE pk_job=?",
                Int::class.java, job2.jobId
            )
        )
    }

    @Test
    fun testDeleteJob() {

        val loc = ProjectFileLocator(
            ProjectStorageEntity.JOB,
            job.id.toString(),
            ProjectStorageCategory.SOURCE,
            "log.txt"
        )

        val spec = ProjectStorageSpec(loc, mapOf("cats" to 100), "logTests".toByteArray())
        projectStorageService.store(spec)

        val logs = projectStorageService.fetch(loc)
        assert(logs.isNotEmpty())

        jobService.deleteJob(job)

        assertEquals(
            0,
            jdbc.queryForObject(
                "SELECT COUNT(0) FROM job WHERE pk_job=?",
                Int::class.java, job.jobId
            )
        )
        assertThrows<ProjectStorageException> {
            projectStorageService.fetch(loc)
        }
    }
}
