package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.archivist.sdk.services.AssetSpec
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AssetServiceTests : AbstractTest() {

    @Before
    fun init() {
        val spec = PipelineSpecV()
        spec.name = "test1"
        spec.type = PipelineType.Import
        spec.processors =
                listOf(ProcessorRef("test.TestProcessor"))
        spec.isStandard = false
        spec.description = "unit test pipeline"
        pipelineService.create(spec)
    }

    @Test
    fun testCreate() {
        val spec = AssetSpec("bilbo.png",
                document = mapOf("foo" to "bar"), pipelines = listOf("test1"))
        val asset = assetService.create(spec)
        val metadata = storageService.getMetadata(asset)
        assertEquals(spec.document, metadata)
    }
}
