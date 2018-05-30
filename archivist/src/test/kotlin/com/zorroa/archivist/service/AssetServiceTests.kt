package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    //lateinit var pipeline : Pipeline

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
    fun testAnalyze() {
        val spec = AssetSpec("bilbo.png", pipelineIds = listOf("test1"))
        val asset = assetService.analyze(spec)
        assertEquals(AssetState.PENDING_FILE, asset.state)
        assertFalse(jdbc.queryForObject("SELECT bool_direct FROM asset WHERE pk_asset=?",
                Boolean::class.java, asset.id))
    }

    @Test
    fun testAnalyzeWithFile() {
        val spec = AssetSpec(location=getTestPath("images/set01/toucan.jpg").toString(),
                pipelineIds = listOf("test1"))
        val asset = assetService.analyze(spec)
        assertEquals(AssetState.PENDING, asset.state)
        assertTrue(jdbc.queryForObject("SELECT bool_direct FROM asset WHERE pk_asset=?",
                Boolean::class.java, asset.id))
    }
}
