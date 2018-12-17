package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getOrgId
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class FileQueueDaoTests : AbstractTest() {

    @Autowired
    lateinit var fileQueueDao: FileQueueDao

    @Autowired
    lateinit var pipelineDao: PipelineDao

    lateinit var pipeline: Pipeline

    @Before
    fun init() {
        pipeline = pipelineDao.create(PipelineSpec("foo", PipelineType.Import, "test", listOf()))
    }

    @Test
    fun testCreate() {
        val org = getOrgId()
        val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(), "/tmp/foo.jpg", mapOf("foo" to "bar"))
        val queued = fileQueueDao.create(spec)
        assertEquals(spec.assetId, queued.assetId)
        assertEquals(spec.organizationId, queued.organizationId)
        assertEquals(spec.path, queued.path)
        assertEquals(spec.metadata, queued.metadata)
    }

    @Test
    fun testDelete() {
        val count = 20
        val files = mutableListOf<QueuedFile>()
        val org = getOrgId()
        for (i in 1..count) {
            val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(),"/tmp/foo$i.jpg", mapOf("foo" to "bar"))
            files.add(fileQueueDao.create(spec))
        }
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(1) FROM queued_file", Int::class.java))
        assertEquals(count, fileQueueDao.delete(files))
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(1) FROM queued_file", Int::class.java))

    }

    @Test
    fun testGetOrgMeters() {
        val org = getOrgId()
        val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(), "/tmp/foo.jpg", mapOf("foo" to "bar"))
        val queued = fileQueueDao.create(spec)

        val meters = fileQueueDao.getOrganizationMeters()
        assertEquals(meters["Zorroa"], 1)
    }

    @Test
    fun testGetQueued() {
        val org = getOrgId()

        for (i in 1..20) {
            val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(),"/tmp/foo$i.jpg", mapOf("foo" to "bar"))
            fileQueueDao.create(spec)
        }

        val queued = fileQueueDao.getAll(100)
        assertEquals(20, queued.size)
    }

    @Test
    fun testGetQueuedSorted() {
        val pipeline2 = pipelineDao.create(PipelineSpec("bar", PipelineType.Import, "test", listOf()))

        val org = getOrgId()

        for (i in 1..5) {
            val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(),"/tmp/foo$i.jpg", mapOf("foo" to "bar"))
            fileQueueDao.create(spec)
        }

        for (i in 1..5) {
            val spec = QueuedFileSpec(org, pipeline2.id, UUID.randomUUID(),"/tmp/foo$i.jpg", mapOf("foo" to "bar"))
            fileQueueDao.create(spec)
        }

        val queued = fileQueueDao.getAll(100)
        assertEquals(10, queued.size)

        // Just validate the pipelines are grouped together.

        if (queued[0].pipelineId == pipeline.id) {
            for (i in 0 .. 4)  {
                assertEquals(queued[i].pipelineId, pipeline.id)
            }

            for (i in 5 .. 9)  {
                assertEquals(queued[i].pipelineId, pipeline2.id)
            }
        }
        else {

            for (i in 0 .. 4)  {
                assertEquals(queued[i].pipelineId, pipeline2.id)
            }

            for (i in 5 .. 9)  {
                assertEquals(queued[i].pipelineId, pipeline.id)
            }
        }

    }
}