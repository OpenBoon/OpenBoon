package boonai.archivist.domain

import org.junit.Test
import kotlin.test.assertEquals

class AssetSpecTests {

    @Test
    fun testCreateWithDocPageNumber() {
        val spec = AssetSpec(
            "gs://foo/bar/bing.jpg",
            document = mutableMapOf("media" to mapOf("pageNumber" to 47))
        )
        assertEquals(47, spec.getPageNumber())
    }

    @Test
    fun testCreateWithPageNumber() {
        val spec = AssetSpec("gs://foo/bar/bing.jpg", page = 92)
        assertEquals(92, spec.getPageNumber())
    }

    @Test
    fun testCreateWithId() {
        val spec = AssetSpec("gs://foo/bar/bing.jpg", id = "abc123")
        assertEquals("abc123", spec.id)
    }

    @Test
    fun testCreateWithNoPage() {
        val spec = AssetSpec("gs://foo/bar/bing.jpg")
        assertEquals(1, spec.getPageNumber())
    }
}
