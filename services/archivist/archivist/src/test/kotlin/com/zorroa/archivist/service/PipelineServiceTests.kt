package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ZpsSlot
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PipelineServiceTests : AbstractTest() {

    @Before
    fun init() {
        pipelineService.create(
            PipelineSpec(
                "source", ZpsSlot.Execute,
                processors = listOf(ProcessorRef("com.zorroa.IngestImages", "foo"))
            )
        )
        pipelineService.create(
            PipelineSpec(
                "ml", ZpsSlot.Execute,
                processors = listOf(ProcessorRef("com.zorroa.Classify", "foo"))
            )
        )
    }

    @Test
    fun testResolvePipelineName() {
        assertEquals(1, pipelineService.resolve("ml").size)
    }

    @Test
    fun testGetPipeline() {
        val pipeline = pipelineService.get("source")
        assertEquals(pipeline.processors.size, 1)
    }

    @Test
    fun testResolveExecutePipeline() {
        val resolved = pipelineService.resolve(
            ZpsSlot.Execute,
            pipelineService.get("source").processors
        )
        assertEquals(1, resolved.size)
    }
}
