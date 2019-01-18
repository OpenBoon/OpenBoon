package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.common.domain.*
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
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

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
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val job2 = jobService.create(spec2)
        assertTrue("admin" in job2.name)
        assertTrue("Import" in job2.name)
    }

    @Test
    fun testIncrementAssetCounts() {
        val counts = BatchCreateAssetsResponse(6)
        counts.createdAssetIds.add("foo")
        counts.replacedAssetIds.addAll(listOf("foo", "bar"))
        counts.erroredAssetIds.addAll(listOf("foo", "bar", "bing"))
        counts.warningAssetIds.addAll(listOf("foo", "bar", "bing", "bang"))
        jobService.incrementAssetCounts(task, counts)

        val map = jdbc.queryForMap("SELECT * FROM task_stat WHERE pk_task=?", task.id)
        assertEquals(counts.createdAssetIds.size, map["int_asset_create_count"])
        assertEquals(counts.replacedAssetIds.size, map["int_asset_replace_count"])
        assertEquals(counts.erroredAssetIds.size, map["int_asset_error_count"])
        assertEquals(counts.warningAssetIds.size, map["int_asset_warning_count"])
        assertEquals(counts.total, map["int_asset_total_count"])

        val map2 = jdbc.queryForMap("SELECT * FROM job_stat WHERE pk_job=?", task.jobId)
        assertEquals(counts.createdAssetIds.size, map2["int_asset_create_count"])
        assertEquals(counts.replacedAssetIds.size, map2["int_asset_replace_count"])
        assertEquals(counts.erroredAssetIds.size, map2["int_asset_error_count"])
        assertEquals(counts.warningAssetIds.size, map2["int_asset_warning_count"])
        assertEquals(counts.total, map2["int_asset_total_count"])
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