package com.zorroa.irm.studio.repository

import com.zorroa.common.domain.PipelineSpec
import com.zorroa.irm.studio.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var pipelineDao: PipelineDao

    val pipelineName = "unit"


    @Test
    fun testCreateAndGetByName() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pipelineName)

        assertEquals(pl2, pl3)
    }

    @Test
    fun testCreateAndGetById() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pl2.id)
        assertEquals(pl2, pl3)
    }

    @Test
    fun testUpdate() {
        val pl1 = pipelineDao.get("test")
        pl1.name = "hello"
        pipelineDao.update(pl1)
        val pl2 =  pipelineDao.get("hello")
    }

    @Test
    fun testExists() {
        assertTrue(pipelineDao.exists("test"))
        assertFalse(pipelineDao.exists("captain kirk"))
    }


    @Test
    fun testCount() {
        val count = pipelineDao.count()
        pipelineDao.create(PipelineSpec(pipelineName))
        assertEquals( pipelineDao.count(), count+1)
    }
}
