package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineUpdate
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PipelineDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var pipelineDao: PipelineDao

    val pipelineName = "unit"

    @Test
    fun testCreateAndGetByName() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pipelineName)
        assertEquals(pl2.id, pl3.id)
    }

    @Test
    fun testCreateAndGetById() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pl2.id)
        assertEquals(pl2.id, pl3.id)
    }

    @Test
    fun testUpdate() {
        val pl1 = pipelineDao.create(PipelineSpec("import-test2"))
        val updated = PipelineUpdate("hello", listOf(), listOf())

        pipelineDao.update(pl1.id, updated)
        val pl2 = pipelineDao.get("hello")
        assertEquals(pl1.id, pl2.id)
    }

    @Test
    fun testCount() {
        val count = pipelineDao.count(PipelineFilter())
        pipelineDao.create(PipelineSpec(pipelineName))
        assertEquals(pipelineDao.count(PipelineFilter()), count + 1)
    }
}
