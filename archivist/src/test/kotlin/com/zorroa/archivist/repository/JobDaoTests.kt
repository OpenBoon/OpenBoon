package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.BatchCreateAssetsResponse
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JobDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

    @Autowired
    internal lateinit var jobService: JobService

    @Test
    fun testCreate() {

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t1 = jobDao.create(spec, PipelineType.Import)
        assertEquals(spec.name, t1.name)
        assertEquals(JobState.Active, t1.state) // no tasks
        assertEquals(PipelineType.Import, t1.type)
    }

    @Test
    fun testUpdate() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))
        val t1 = jobDao.create(spec, PipelineType.Import)
        val update = JobUpdateSpec("bilbo_baggins", 5)
        assertTrue(jobDao.update(t1, update))
        val t2 = jobDao.get(t1.id)
        assertEquals(update.name, t2.name)
        assertEquals(update.priority, t2.priority)
    }

    @Test
    fun testSetTimeStarted() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))
        val t1 = jobDao.create(spec, PipelineType.Import)
        assertTrue(jobDao.setTimeStarted(t1))
        assertFalse(jobDao.setTimeStarted(t1))
        val time = jdbc.queryForObject("SELECT time_started FROM job WHERE pk_job=?", Long::class.java, t1.jobId)
        assertTrue(time != -1L)
    }

    @Test
    fun testGet() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec, PipelineType.Import)
        val t1 = jobDao.get(t2.id)

        assertEquals(t2.name, t1.name)
        assertEquals(t2.organizationId, t1.organizationId)
        assertEquals(t2.state, t1.state)
        assertEquals(t2.type, t1.type)
    }

    @Test
    fun getTestForClient() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec, PipelineType.Import)
        val t1 = jobDao.get(t2.id, forClient = true)
        assertNotNull(t1.assetCounts)
        assertNotNull(t1.taskCounts)
        assertNotNull(t1.createdUser)
    }


    @Test
    fun getTestWeHaveTimestampsForClient() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec, PipelineType.Import)
        val t1 = jobDao.get(t2.id, forClient = true)
        assertNotNull(t1.timeStarted)
        assertNotNull(t1.timeUpdated)
    }

    @Test
    fun testIncrementAssetStats() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job1 = jobDao.create(spec, PipelineType.Import)
        val counts = BatchCreateAssetsResponse(6)
        counts.createdAssetIds.add("foo")
        counts.replacedAssetIds.addAll(listOf("foo", "bar"))
        counts.erroredAssetIds.addAll(listOf("foo", "bar", "bing"))
        counts.warningAssetIds.addAll(listOf("foo", "bar", "bing", "bang"))
        assertTrue(jobDao.incrementAssetStats(job1, counts))

        val map = jdbc.queryForMap("SELECT * FROM job_stat WHERE pk_job=?", job1.id)
        print(map)
        assertEquals(counts.createdAssetIds.size, map["int_asset_create_count"])
        assertEquals(counts.replacedAssetIds.size, map["int_asset_replace_count"])
        assertEquals(counts.erroredAssetIds.size, map["int_asset_error_count"])
        assertEquals(counts.warningAssetIds.size, map["int_asset_warning_count"])
        assertEquals(counts.total, map["int_asset_total_count"])
    }

    @Test
    fun testGetAllWithFilter() {
        val orgId = UUID.randomUUID()
        for (i in 1..10) {
            val spec = JobSpec("run_some_stuff_$i",
                    emptyZpsScript("test_script"))
            jobDao.create(spec, PipelineType.Import)
        }

        var filter = JobFilter(organizationIds=listOf(UUID.randomUUID()))
        var jobs = jobDao.getAll(filter)
        assertEquals(0, jobs.size())
        assertEquals(0, jobs.page.totalCount)

        filter = JobFilter(organizationIds=listOf(orgId, getOrgId()))
        jobs = jobDao.getAll(filter)
        assertEquals(10, jobs.size())
        assertEquals(10, jobs.page.totalCount)
    }

    @Test
    fun testAllSortColumns() {
        for (i in 1..10) {
            val random = Random.nextInt(1,100000)
            val spec = JobSpec("run_some_stuff_$random",
                    emptyZpsScript("test_script"))
            jobDao.create(spec, PipelineType.Import)
        }

        // All the columns we can sort by.
        val sortFields = listOf(
            "id", "type", "name", "timeCreated", "state", "priority", "organizationId"
        )

        // Just test the DB allows us to sort
        for (field in sortFields) {
            var filter = JobFilter().apply {
                sort = listOf("$field:a")
            }
            val page = jobDao.getAll(filter)
            assertTrue(page.size() > 0)
        }
    }


    @Test
    fun testDelete() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobDao.create(spec, PipelineType.Import)
        assertTrue(jobDao.delete(job))
        assertFalse(jobDao.delete(job))

    }

    @Test
    fun testHasPendingFrames() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        TaskState.Skipped
        val job = jobService.create(spec, PipelineType.Import)
        assertTrue(jobDao.hasPendingFrames(job))
        jdbc.update("UPDATE job_count SET int_task_state_0=0, int_task_state_4=1")
        assertFalse(jobDao.hasPendingFrames(job))
    }

    @Test
    fun testGetExpired() {
        assertTrue(jobDao.getExpired(1, TimeUnit.DAYS, 100).isEmpty())

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val job = jobDao.create(spec, PipelineType.Import)
        assertTrue(jobDao.setState(job, JobState.Finished, null))
        Thread.sleep(100)
        assertTrue(jobDao.getExpired(101, TimeUnit.MILLISECONDS, 100).isNotEmpty())
        assertTrue(jobDao.getExpired(1, TimeUnit.DAYS, 100).isEmpty())
    }
}
