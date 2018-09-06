package com.zorroa.analyst.repository

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.domain.*
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

    @Test
    fun testCreate() {

        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("foo"),
                attrs=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t1 = jobDao.create(spec)
        assertEquals(spec.name, t1.name)
        assertEquals(spec.organizationId, t1.organizationId)
        assertEquals(1, t1.attrs.get("foo"))
        assertEquals("bar", t1.env.get("foo"))
        assertEquals(spec.lockAssets, t1.lockAssets)
        assertEquals(JobState.Setup, t1.state)
        assertEquals(PipelineType.Import, t1.type)
        assertEquals("zorroa/jobs/${t1.id}/script.zps", t1.getScriptPath())
    }

    @Test
    fun testGet() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("test_script"),
                attrs=mutableMapOf("foo" to 1),
                env=mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec)
        val t1 = jobDao.get(t2.id)

        assertEquals(t2.name, t1.name)
        assertEquals(t2.organizationId, t1.organizationId)
        assertEquals(t2.attrs["foo"], t1.attrs["foo"])
        assertEquals(t2.env["foo"], t1.env["foo"])
        assertEquals(t2.lockAssets, t1.lockAssets)
        assertEquals(t2.state, t1.state)
        assertEquals(t2.type, t1.type)
        assertEquals("zorroa/jobs/${t1.id}/script.zps", t1.getScriptPath())
    }

    @Test
    fun testGetRunning() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("test_script"))
        val t1 = jobDao.create(spec)
        assertTrue(jobDao.setState(t1, JobState.Running, null))
        assertTrue(jobDao.getRunning().isNotEmpty())
    }

    @Test
    fun testGetWaiting() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("test_script"))
        val t1 = jobDao.create(spec)
        jobDao.setState(t1, JobState.Waiting, null)
        val all = jobDao.getWaiting(10)
        assertTrue(all.isNotEmpty())
        assertEquals(t1.id, all[0].id)
    }

    @Test
    fun testGetOrphans() {
        val spec = JobSpec("test_job",
                PipelineType.Import,
                UUID.randomUUID(),
                ZpsScript("test_script"))
        val t1 = jobDao.create(spec)
        jobDao.setState(t1, JobState.Queue, null)
        jdbc.update("UPDATE job SET time_modified=? WHERE pk_job=?",
                System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(6),
                t1.id)

        val all = jobDao.getOrphans()
        assertEquals(1, all.size)
    }

    @Test
    fun testGetAllWithFilter() {
        val orgId = UUID.randomUUID()
        for (i in 1..10) {
            val spec = JobSpec("run_some_stuff_$i",
                    PipelineType.Import,
                    orgId,
                    ZpsScript("test_script"))
            jobDao.create(spec)
        }

        var filter = JobFilter(organizationIds= listOf(UUID.randomUUID()))
        var jobs = jobDao.getAll(filter)
        assertEquals(0, jobs.size())
        assertEquals(0, jobs.page.totalCount)

        filter = JobFilter(organizationIds= listOf(orgId, UUID.randomUUID()))
        jobs = jobDao.getAll(filter)
        assertEquals(10, jobs.size())
        assertEquals(10, jobs.page.totalCount)
    }
}
