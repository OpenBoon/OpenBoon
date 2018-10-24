package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.ProcessorRef
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class PipelineServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    @Before
    fun init() {
        pipelineService.create(PipelineSpec("source", PipelineType.Import, "test",
                processors = listOf(ProcessorRef("com.zorroa.IngestImages"))))
        pipelineService.create(PipelineSpec("ml", PipelineType.Import, "test",
                processors = listOf(ProcessorRef("com.zorroa.Classify"))))
    }

    @Test
    fun testResolvePipelineName() {
        assertEquals(1, pipelineService.resolve("ml").size)
    }

    @Test
    fun testGetDefaultPipelines() {
        assertEquals("standard-import", pipelineService.getDefaultPipelineName(PipelineType.Import))
    }

    @Test
    fun testResolveDefaultPipeline() {
        assertEquals(1, pipelineService.resolveDefault(PipelineType.Import).size)
    }

    @Test
    fun testResolveImportPipeline() {
        val resolved = pipelineService.resolve(PipelineType.Import,
                pipelineService.get("standard-import").processors)
        assertEquals(1, resolved.size)
    }

    @Test
    fun testResolveExportPipeline() {
        val resolved = pipelineService.resolve(PipelineType.Import,
                pipelineService.get("standard-export").processors)
        assertEquals(1, resolved.size)
    }

    @Test
    fun resolveEmbeddedPipeline() {
        val refs = pipelineService.resolve("embedded")
        assertEquals(2, refs.size)
    }
}
