package com.zorroa.analyst.service

import com.zorroa.analyst.AbstractTest
import com.zorroa.common.domain.PipelineSpec
import com.zorroa.common.domain.PipelineType
import com.zorroa.common.domain.ProcessorRef
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PipelineServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    @Before
    fun setup() {
        pipelineService.create(PipelineSpec("source", PipelineType.IMPORT,
                processors = listOf(ProcessorRef("com.zorroa.IngestImages"))))
        pipelineService.create(PipelineSpec("ml", PipelineType.IMPORT,
                processors = listOf(ProcessorRef("com.zorroa.Classify"))))
    }

    @Test
    fun testBuilProcessorList() {
        assertEquals(2, pipelineService.resolveExecute(listOf("source", "ml")).size)
    }

    @Test
    fun testGetDefaultPipelines() {
        assertEquals(1, pipelineService.getDefaultPipelineNames(PipelineType.IMPORT).size)
    }

}
