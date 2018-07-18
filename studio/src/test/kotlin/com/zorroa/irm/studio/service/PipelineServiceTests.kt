package com.zorroa.irm.studio.service

import com.zorroa.common.domain.PipelineSpec
import com.zorroa.common.domain.ProcessorRef
import com.zorroa.irm.studio.AbstractTest
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PipelineServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    @Before
    fun setup() {
        pipelineService.create(PipelineSpec("source", 1,
                processors = listOf(ProcessorRef("com.zorroa.IngestImages"))))
        pipelineService.create(PipelineSpec("ml", 1,
                processors = listOf(ProcessorRef("com.zorroa.Classify"))))
    }

    @Test
    fun testBuilProcessorList() {
        assertEquals(2, pipelineService.buildProcessorList(listOf("source", "ml")).size)
    }

    @Test
    fun testGetDefaultPipelines() {
        assertEquals(1, pipelineService.getDefaultPipelineList().size)
    }

}
