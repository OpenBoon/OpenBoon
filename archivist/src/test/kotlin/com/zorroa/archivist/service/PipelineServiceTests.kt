package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.ProcessorType
import org.junit.Before
import org.junit.Test
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PipelineServiceTests : AbstractTest() {

    internal lateinit var pipeline: Pipeline
    internal lateinit var spec: PipelineSpecV

    @Before
    fun init() {
        spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(
                ProcessorRef("com.zorroa.core.common.GroupProcessor"))
        spec.description = "A NoOp"
        spec.name = "test"
        spec.type = PipelineType.Import
        pipeline = pipelineService.create(spec)
    }

    @Test
    fun testCreateNestedGenerate() {

        val ref = ProcessorRef("com.zorroa.core.generator.FileListGenerator")
        ref.execute = Lists.newArrayList(ProcessorRef("com.zorroa.core.common.GroupProcessor"))

        spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(ref)
        spec.description = "A NoOp"
        spec.name = "foo"
        spec.type = PipelineType.Generate
        val pl = pipelineService.create(spec)

        assertEquals(PipelineType.Generate, pl.getType())

        val gen = pl.getProcessors().get(0)
        assertEquals(ProcessorType.Generate, gen.getType())

        val exec = gen.getExecute().get(0)
        assertEquals(ProcessorType.Common, exec.getType())
    }

    @Test(expected = IllegalStateException::class)
    fun testCreateNestedGenerateFailure() {

        val ref = ProcessorRef("com.zorroa.core.generator.FileListGenerator")
        ref.execute = Lists.newArrayList(ProcessorRef("com.zorroa.core.generator.FileListGenerator"))

        spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(ref)
        spec.description = "A NoOp"
        spec.name = "foo"
        spec.type = PipelineType.Generate
        pipelineService!!.create(spec)
    }

    @Test
    fun testGet() {
        val p1 = pipelineService.get(pipeline.id)
        val p2 = pipelineService.get(pipeline.name)
        assertEquals(p1, p2)
        assertEquals(p1, pipeline)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testDelete() {
        assertTrue(pipelineService.delete(pipeline.id))
        pipelineService.get(pipeline.id)
    }

    @Test
    fun testGetAll() {
        val current = pipelineService.getAll().size
        spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(
                ProcessorRef("com.zorroa.core.common.GroupProcessor"))
        spec.description = "Test"
        spec.name = "test2"
        spec.type = PipelineType.Import
        pipelineService.create(spec)

        assertEquals((current + 1).toLong(), pipelineService.getAll().size.toLong())
    }

    @Test
    fun testExists() {
        assertTrue(pipelineService.exists("test"))
        assertFalse(pipelineService.exists("false"))
    }

    @Test
    fun testUpdate() {
        pipeline.name = "foo"
        pipeline.description = "bar"
        pipeline.type = PipelineType.Export
        pipeline.processors = Lists.newArrayList()
        assertTrue(pipelineService.update(pipeline.id, pipeline))

        val p = pipelineService.get(pipeline.id)
        assertEquals(pipeline.name, p.getName())
        assertEquals(pipeline.description, p.getDescription())
        assertEquals(pipeline.processors, p.getProcessors())
    }
}
