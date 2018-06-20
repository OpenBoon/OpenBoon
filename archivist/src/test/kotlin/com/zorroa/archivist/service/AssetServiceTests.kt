package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.common.domain.AssetSpec
import org.junit.Test
import kotlin.test.assertEquals

class AssetServiceTests : AbstractTest() {

    @Test
    fun testCreate() {
        val spec = AssetSpec("bilbo.png",
                document = mapOf("foo" to "bar"), pipelines = listOf("test1"))
        val asset = assetService.create(spec)
        val metadata = storageService.getMetadata(asset)
        assertEquals(spec.document, metadata)
    }
}
