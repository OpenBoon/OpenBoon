package com.zorroa.analyst.service

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.domain.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.web.WebAppConfiguration
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@WebAppConfiguration
class JobServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Test
    fun testCreate() {
        val assetId = UUID.randomUUID().toString()
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("foo", over=mutableListOf(Document(assetId))),
                lockAssets = true)
        val job = jobService.create(spec)

        // Since lockAssets = true, the asset IDs should be in the mapping table.
        val count = jdbc.queryForObject(
                "SELECT COUNT(1) FROM x_asset_job WHERE pk_asset=?::uuid AND pk_job=?",
                Int::class.java, assetId, job.id)
        assertEquals(1, count)
    }

    @Test
    fun testStart() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("foo"),
                attrs=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        var job = jobService.create(spec)
        jobService.setState(job, JobState.Queue, null)
        jobService.start(job)
        job = jobService.get(job.id)
        assertEquals(JobState.Running, job.state)
    }

    @Test
    fun testGet() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("foo"))
        var job1 = jobService.create(spec)
        var job2 = jobService.get(job1.id)
        var job3 = jobService.get(job1.name)
        assertEquals(job1.id, job2.id)
        assertEquals(job1.id, job3.id)
    }


    @Test
    fun testSetState() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("foo"))
        var job1 = jobService.create(spec)
        assertTrue(jobService.setState(job1, JobState.Fail))
        assertFalse(jobService.setState(job1, JobState.Fail, JobState.Running))
        assertTrue(jobService.setState(job1, JobState.Running, JobState.Fail))
    }

    @Test
    fun testStop() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("foo"))
        var job = jobService.create(spec)
        jobService.setState(job, JobState.Queue)
        jobService.start(job)

        assertTrue(jobService.stop(job, JobState.Success))

        val startTime = jdbc.queryForObject(
                "SELECT time_started FROM job WHERE pk_job=?", Long::class.java, job.id)
        assertTrue(startTime > -1)

        val stopTime = jdbc.queryForObject(
                "SELECT time_stopped FROM job WHERE pk_job=?", Long::class.java, job.id)
        assertTrue(stopTime > -1)

    }
}
