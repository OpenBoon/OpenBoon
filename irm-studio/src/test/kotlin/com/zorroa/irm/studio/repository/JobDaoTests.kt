package com.zorroa.irm.studio.repository

import com.zorroa.common.domain.JobSpec
import com.zorroa.irm.studio.AbstractTest
import com.zorroa.irm.studio.service.PipelineService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class JobDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var jobDao: JobDao

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
                pipelineService.getDefaultPipelineList())
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
}
