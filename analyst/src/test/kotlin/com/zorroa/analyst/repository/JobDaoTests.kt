package com.zorroa.analyst.repository

import com.zorroa.analyst.AbstractTest
import com.zorroa.analyst.domain.LockSpec
import com.zorroa.analyst.service.PipelineService
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.PipelineType
import com.zorroa.common.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

    @Autowired
    internal lateinit var lockDao: LockDao

    @Autowired
    internal lateinit var pipelineService: PipelineService

    @Test
    fun testCreateAndGet() {
        val spec = JobSpec("run_some_stuff",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"),
                mapOf("foo" to 1),
                mapOf("foo" to "bar"))

        val t1 = jobDao.create(spec)
        assertEquals(spec.name, t1.name)
        assertEquals(spec.assetId, t1.assetId)
        assertEquals(spec.pipelines, t1.pipelines)
        assertEquals(1, t1.pipelines.size)
        assertEquals(1, t1.attrs.get("foo"))
        assertEquals("bar", t1.env.get("foo"))
    }

    @Test
    fun testCreateWithDefaultPipelines() {
        val spec = JobSpec("run_some_stuff",
                UUID.randomUUID(),
                UUID.randomUUID(),
                pipelineService.getDefaultPipelineNames(PipelineType.IMPORT))
        val t1 = jobDao.create(spec)
        assertEquals(spec.name, t1.name)
        assertEquals(spec.assetId, t1.assetId)
        assertEquals(spec.pipelines, t1.pipelines)
        assertEquals(1, t1.pipelines.size)
    }

    @Test
    fun testGet() {
        val spec = JobSpec("run_some_stuff",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t1 = jobDao.create(spec)
        val t2 = jobDao.get(t1.id)

        assertEquals(spec.name, t2.name)
        assertEquals(spec.assetId, t2.assetId)
        assertEquals(spec.pipelines, t2.pipelines)
        assertEquals(1, t2.pipelines.size)
    }

    @Test
    fun testGetRunning() {
        val spec = JobSpec("run_some_stuff",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t1 = jobDao.create(spec)
        assertTrue(jobDao.setState(t1, JobState.RUNNING, JobState.WAITING))
        val all = jobDao.getRunning()
        assertTrue(all.isNotEmpty())
    }

    @Test
    fun testGetWaiting() {
        val spec = JobSpec("run_some_stuff",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t1 = jobDao.create(spec)
        val all = jobDao.getWaiting(10)
        assertTrue(all.contains(t1))
    }

    @Test
    fun testGetWaitingButLocked() {
        val spec = JobSpec("run_some_stuff",
                UUID.randomUUID(),
                UUID.randomUUID(),
                listOf("standard"))
        val t1 = jobDao.create(spec)
        val l1 = lockDao.create(LockSpec(t1))

        val all = jobDao.getWaiting(10)
        assertTrue(all.isEmpty())
    }

    @Test
    fun testGetAllWithFilter() {
        val assetId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        for (i in 1..10) {
            val spec = JobSpec("run_some_stuff_v$i",
                    assetId,
                    orgId,
                    listOf("standard"))
            jobDao.create(spec)
        }

        var filter = JobFilter(assetIds=listOf(assetId))
        var jobs = jobDao.getAll(filter)
        assertEquals(10, jobs.size())
        assertEquals(10, jobs.page.totalCount)

        filter = JobFilter(assetIds=listOf(assetId), organizationIds =listOf(UUID.randomUUID()))
        jobs = jobDao.getAll(filter)
        assertEquals(0, jobs.size())

        logger.info(Json.prettyString(jobs))

    }
}
