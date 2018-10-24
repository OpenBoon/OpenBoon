package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.domain.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JobDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

    @Test
    fun testCreate() {

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t1 = jobDao.create(spec, PipelineType.Import)
        assertEquals(spec.name, t1.name)
        assertEquals(JobState.Finished, t1.state) // no tasks
        assertEquals(PipelineType.Import, t1.type)
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
        val counts = AssetIndexResult()
        counts.created = 1
        counts.replaced = 2
        counts.errors = 3
        counts.warnings = 4
        counts.updated = 5
        counts.total = 11
        assertTrue(jobDao.incrementAssetStats(job1, counts))

        val map = jdbc.queryForMap("SELECT * FROM job_stat WHERE pk_job=?", job1.id)
        print(map)
        assertEquals(counts.created, map["int_asset_create_count"])
        assertEquals(counts.replaced, map["int_asset_replace_count"])
        assertEquals(counts.errors, map["int_asset_error_count"])
        assertEquals(counts.warnings, map["int_asset_warning_count"])
        assertEquals(counts.updated, map["int_asset_update_count"])
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
        var jobs = jobDao.getAll(Pager.first(), filter)
        assertEquals(0, jobs.size())
        assertEquals(0, jobs.page.totalCount)

        filter = JobFilter(organizationIds=listOf(orgId, getOrgId()))
        jobs = jobDao.getAll(Pager.first(), filter)
        assertEquals(10, jobs.size())
        assertEquals(10, jobs.page.totalCount)
    }
}
