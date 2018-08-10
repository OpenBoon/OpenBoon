package com.zorroa.analyst.repository

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.domain.PipelineSpec
import com.zorroa.common.domain.PipelineType
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
        val pl1 = PipelineSpec(pipelineName, PipelineType.Import)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pipelineName)
        assertEquals(pl2, pl3)
    }

    @Test
    fun testCreateAndGetById() {
        val pl1 = PipelineSpec(pipelineName, PipelineType.Import)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pl2.id)
        assertEquals(pl2, pl3)
    }

    @Test
    fun testUpdate() {
        val pl1 = pipelineDao.create(PipelineSpec("import-test2", PipelineType.Import))
        pl1.name = "hello"
        pipelineDao.update(pl1)
        val pl2 =  pipelineDao.get("hello")
        assertEquals(pl1.id, pl2.id)
    }

    @Test
    fun testExists() {
        pipelineDao.create(PipelineSpec("export-test2", PipelineType.Export))
        assertTrue(pipelineDao.exists("export-test2"))
        assertFalse(pipelineDao.exists("captain kirk"))
    }


    @Test
    fun testCount() {
        val count = pipelineDao.count()
        pipelineDao.create(PipelineSpec(pipelineName, PipelineType.Import))
        assertEquals( pipelineDao.count(), count+1)
    }
}
