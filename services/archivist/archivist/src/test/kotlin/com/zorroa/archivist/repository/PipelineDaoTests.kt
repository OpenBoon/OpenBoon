package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ZpsSlot
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PipelineDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var pipelineDao: PipelineDao

    val pipelineName = "unit"

    @Test
    fun testCreateAndGetByName() {
        val pl1 = PipelineSpec(pipelineName, ZpsSlot.Execute)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pipelineName)
        assertEquals(pl2.id, pl3.id)
    }

    @Test
    fun testCreateAndGetById() {
        val pl1 = PipelineSpec(pipelineName, ZpsSlot.Execute)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pl2.id)
        assertEquals(pl2.id, pl3.id)
    }

    @Test
    fun testUpdate() {
        val pl1 = pipelineDao.create(PipelineSpec("import-test2", ZpsSlot.Execute))
        pl1.name = "hello"
        pipelineDao.update(pl1)
        val pl2 = pipelineDao.get("hello")
        assertEquals(pl1.id, pl2.id)
    }

    @Test
    fun testCount() {
        val count = pipelineDao.count(PipelineFilter())
        pipelineDao.create(PipelineSpec(pipelineName, ZpsSlot.Execute))
        assertEquals(pipelineDao.count(PipelineFilter()), count + 1)
    }
}
