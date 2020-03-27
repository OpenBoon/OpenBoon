package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.ModType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.service.PipelineModService
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipelineModDaoTests : AbstractTest() {

    @Autowired
    lateinit var pipelineModDao: PipelineModDao

    @Autowired
    lateinit var pipelineModService: PipelineModService

    lateinit var module: PipelineMod

    @Before
    fun init() {
        val spec = PipelineModSpec("foo", "test",
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModType.LABEL_DETECTION,
            listOf(), listOf(), false)
        module = pipelineModService.create(spec)
    }
    @Test
    fun testFindByIdIn() {
        assertTrue(pipelineModDao.findByIdIn(listOf(UUID.randomUUID())).isEmpty())
        assertFalse(pipelineModDao.findByIdIn(listOf(module.id)).isEmpty())
    }

    @Test
    fun getByName() {
        assertNull(pipelineModDao.getByName("dog"))
        assertNotNull(pipelineModDao.getByName("foo"))
    }
}
