package boonai.archivist.domain

import boonai.archivist.AbstractTest
import org.junit.Test
import kotlin.test.assertEquals

class AssetIdBuilderTests : AbstractTest() {

    @Test
    fun testBuild() {
        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        val builder = AssetIdBuilder(spec)
        val id = builder.build()
        assertEquals("jvfC_RFfhDKDXgqmgTFdtszLXj15uP-3", id)
    }

    @Test
    fun testBuildWithChecksum() {
        val spec = AssetSpec("gs://cats/large-brown-cat.jpg")
        spec.makeChecksum("peaches".toByteArray())
        val builder = AssetIdBuilder(spec)
        val id = builder.build()
        assertEquals("YRwTuSPS7xJ_gWUdjaYHEZPCf13Yz-yb", id)
    }
}
