package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.Task
import com.zorroa.common.domain.TaskSpec
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JobServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    lateinit var spec: JobSpec
    lateinit var job: Job
    lateinit var task: Task

    @Before
    fun init() {
        spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        job = jobService.create(spec)
        task = jobService.createTask(job, TaskSpec("bar", emptyZpsScript("bar")))
    }

    @Test
    fun testCreate() {
        assertEquals(spec.name, job.name)
        val tcount = jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE pk_job=?", Int::class.java, job.id)
        assertEquals(2, tcount)
    }

    @Test
    fun testCreateWithAutoName() {
        val spec2 = JobSpec(null,
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))
        val job2 = jobService.create(spec2)
        assertTrue("admin" in job2.name)
        assertTrue("Import" in job2.name)
    }

    @Test
    fun testReplace() {
        val name = "bilbo_baggins_v1"
        val spec1 = JobSpec(name,
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))
        val job1 = jobService.create(spec1)
        assertEquals(spec1.name, job1.name)

        val spec2 = JobSpec(name,
                emptyZpsScript("foo"),
                replace = true)
        val job2 = jobService.create(spec2)
        assertEquals(job1.name, job2.name)

        // Should only have 1 job.
        assertEquals(1, jobService.getAll(
            JobFilter(
                states = listOf(JobState.Active),
                names = listOf(job1.name))
        ).size())
    }

    @Test
    fun testIncrementAssetCounts() {

        val counters = AssetCounters(
                total = 10,
                errors = 6,
                replaced = 4,
                warnings = 2,
                created = 6)

        jobService.incrementAssetCounters(task, counters)

        val map = jdbc.queryForMap("SELECT * FROM task_stat WHERE pk_task=?", task.id)
        assertEquals(counters.created, map["int_asset_create_count"])
        assertEquals(counters.replaced, map["int_asset_replace_count"])
        assertEquals(counters.errors, map["int_asset_error_count"])
        assertEquals(counters.warnings, map["int_asset_warning_count"])
        assertEquals(counters.warnings, map["int_asset_warning_count"])

        val map2 = jdbc.queryForMap("SELECT * FROM job_stat WHERE pk_job=?", task.jobId)
        assertEquals(counters.created, map2["int_asset_create_count"])
        assertEquals(counters.replaced, map2["int_asset_replace_count"])
        assertEquals(counters.errors, map2["int_asset_error_count"])
        assertEquals(counters.warnings, map2["int_asset_warning_count"])
        assertEquals(counters.total, map2["int_asset_total_count"])
    }

    @Test
    fun checkAndSetJobFinished() {
        assertFalse(jobService.checkAndSetJobFinished(job))
        jdbc.update("UPDATE job_count SET int_task_state_0=0, int_task_state_4=2")
        assertTrue(jobService.checkAndSetJobFinished(job))
        val job2 = jobService.get(job.jobId)
        assertEquals(JobState.Finished, job2.state)
    }
}
