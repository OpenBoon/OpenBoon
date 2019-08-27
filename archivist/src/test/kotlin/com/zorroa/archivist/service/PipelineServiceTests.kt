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
        pipelineService.create(
            PipelineSpec(
                "source", PipelineType.Import, "test",
                processors = listOf(ProcessorRef("com.zorroa.IngestImages"))
            )
        )
        pipelineService.create(
            PipelineSpec(
                "ml", PipelineType.Import, "test",
                processors = listOf(ProcessorRef("com.zorroa.Classify"))
            )
        )
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
    fun testGetPipeline() {
        val pipeline = pipelineService.get("standard-import")
        assertEquals(pipeline.processors.size, 7)
    }

    @Test
    fun testResolveDefaultPipeline() {
        assertEquals(7, pipelineService.resolveDefault(PipelineType.Import).size)
    }

    @Test
    fun testResolveImportPipeline() {
        val resolved = pipelineService.resolve(
            PipelineType.Import,
            pipelineService.get("standard-import").processors
        )
        assertEquals(7, resolved.size)
    }

    @Test
    fun testResolveExportPipeline() {
        val resolved = pipelineService.resolve(
            PipelineType.Import,
            pipelineService.get("standard-export").processors
        )
        assertEquals(1, resolved.size)
    }

    @Test
    fun testLoadFragmentExternalFile() {
        val frag = pipelineService.loadFragment("src/test/resources/pipeline_fragment.json")
        assertEquals(3, frag.size)
        assertEquals("zplugins.core.collectors.GoogleCloudStorageCollector", frag[0].className)
        assertEquals("gs://zorroa/foo/bar", frag[0].args!!["bucket"])
    }

    @Test
    fun testLoadFragmentString() {
        val frag = pipelineService.loadFragment("foo.bar.Bing,foo.bar.Bong")
        assertEquals(2, frag.size)
        assertEquals("foo.bar.Bing", frag[0].className)
        assertEquals("foo.bar.Bong", frag[1].className)
    }
}
