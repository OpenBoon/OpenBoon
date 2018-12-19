package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineType
import com.zorroa.archivist.domain.QueuedFileSpec
import com.zorroa.archivist.security.getOrgId
import io.micrometer.core.instrument.MeterRegistry
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import kotlin.test.assertEquals

class FileQueueServiceTests : AbstractTest() {

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var fileQueueService: FileQueueService

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    lateinit var pipeline: Pipeline

    @Before
    fun init() {
        pipeline = pipelineService.create(
                PipelineSpec("foo", PipelineType.Import, "test", listOf()))
    }

    @Test
    fun testProcessQueueEmptyQueue() {
        assertEquals(0, fileQueueService.processQueue())
    }

    @Test
    fun testProcessQueue() {
        val org = getOrgId()
        val spec = QueuedFileSpec(org, pipeline.id, UUID.randomUUID(), "/tmp/foo.jpg", mapOf("foo" to "bar"))
        fileQueueService.create(spec)
        fileQueueService.create(spec)

        assertEquals(2, fileQueueService.processQueue())
    }
}