package com.zorroa.archivist.service

import com.google.common.collect.Lists
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
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
    fun testGet() {
        val p1 = pipelineService.get(pipeline.id)
        val p2 = pipelineService.get(pipeline.name)
        assertEquals(p1, p2)
        assertEquals(p1, pipeline)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testDelete() {
        val spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(
                ProcessorRef("com.zorroa.core.common.GroupProcessor"))
        spec.description = "boo"
        spec.name = "boo"
        spec.type = PipelineType.Import
        val pipeline = pipelineService.create(spec)

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

    @Test
    fun testCreateStandard() {
        pipelineService.getStandard(PipelineType.Import)

        val spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(
                ProcessorRef("com.zorroa.core.common.GroupProcessor"))
        spec.description = "desc"
        spec.name = "standard"
        spec.type = PipelineType.Import
        spec.isStandard = true
        val pipe = pipelineService.create(spec)
        assertTrue(pipe.isStandard)
    }

    @Test
    fun testUpdateStandardFlag() {
        val spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(
                ProcessorRef("com.zorroa.core.common.GroupProcessor"))
        spec.description = "desc"
        spec.name = "standard"
        spec.type = PipelineType.Import
        spec.isStandard = false
        val pipe = pipelineService.create(spec)
        assertFalse(pipe.isStandard)

        pipe.isStandard = true
        assertTrue(pipelineService.update(pipe.id, pipe))

        val pipe2 = pipelineService.get(pipe.id)
        assertTrue(pipe2.isStandard, "Pipeline2 should be standard")
    }

    @Test
    fun testUpdateStandardFailure() {
        val spec = PipelineSpecV()
        spec.processors = Lists.newArrayList(
                ProcessorRef("com.zorroa.core.common.GroupProcessor"))
        spec.description = "desc"
        spec.name = "standard"
        spec.type = PipelineType.Import
        spec.isStandard = true
        val pipe = pipelineService.create(spec)
        assertTrue(pipe.isStandard)

        // Setting the standard to false should not update the standard.
        // because to choose a new standard you always set the new one
        // to true
        pipe.isStandard = false
        assertTrue(pipelineService.update(pipe.id, pipe))

        val pipe2 = pipelineService.get(pipe.id)
        assertTrue(pipe2.isStandard, "Pipeline2 is not standard")
    }
}
