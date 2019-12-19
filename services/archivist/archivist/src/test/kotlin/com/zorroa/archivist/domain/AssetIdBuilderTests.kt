package com.zorroa.archivist.domain

import com.zorroa.archivist.AbstractTest
import org.junit.Test
import kotlin.test.assertEquals

class AssetIdBuilderTests : AbstractTest() {

    @Test
    fun testBuild() {
        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        val builder = AssetIdBuilder(spec)
        val id = builder.build()
        assertEquals("hWsGI5FlI2HRdbT6EzJ3HqF3IA0WBx2_", id)
    }

    @Test
    fun testBuildWithChecksum() {
        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        val builder = AssetIdBuilder(spec)
            .checksum("peaches".toByteArray())
        val id = builder.build()
        assertEquals("YKwzMmtXGTZ9fyT5QXCqUwPoht3ex38D", id)
    }
}