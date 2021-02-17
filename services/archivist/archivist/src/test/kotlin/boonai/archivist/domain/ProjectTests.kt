package boonai.archivist.domain

import org.junit.Test
import kotlin.test.assertEquals

class ProjectTests

class ProjectQuotaCountersTests {

    @Test
    fun testCountImage() {
        val asset = Asset("foo")
        asset.setAttr("aux.field", 1)
        asset.setAttr("media.type", "image")
        asset.setAttr("media.length", 10.732)
        asset.setAttr("clip.type", "full")

        val counters = ProjectQuotaCounters()
        counters.count(asset)

        assertEquals(1, counters.pageCount)
        assertEquals(1, counters.imageFileCount)
        assertEquals(0.0, counters.videoLength)
        assertEquals(0, counters.videoClipCount)
        assertEquals(0, counters.videoFileCount)
        assertEquals(0, counters.documentFileCount)
    }

    @Test
    fun testCountDocument() {
        val asset = Asset("foo")
        asset.setAttr("aux.field", 1)
        asset.setAttr("media.type", "document")
        asset.setAttr("media.length", 10.732)
        asset.setAttr("clip.type", "full")

        val counters = ProjectQuotaCounters()
        counters.count(asset)

        assertEquals(1, counters.pageCount)
        assertEquals(0, counters.imageFileCount)
        assertEquals(0.0, counters.videoLength)
        assertEquals(0, counters.videoClipCount)
        assertEquals(0, counters.videoFileCount)
        assertEquals(1, counters.documentFileCount)
    }

    @Test
    fun testCountVideo() {
        val asset = Asset("foo")
        asset.setAttr("aux.field", 1)
        asset.setAttr("media.type", "video")
        asset.setAttr("media.length", 5.5)
        asset.setAttr("clip.type", "full")

        val counters = ProjectQuotaCounters()
        counters.count(asset)

        assertEquals(0, counters.pageCount)
        assertEquals(0, counters.imageFileCount)
        assertEquals(5.5, counters.videoLength)
        assertEquals(1, counters.videoClipCount)
        assertEquals(1, counters.videoFileCount)
        assertEquals(0, counters.documentFileCount)
    }
}
