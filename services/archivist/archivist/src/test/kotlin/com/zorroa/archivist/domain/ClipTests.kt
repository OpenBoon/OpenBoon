package com.zorroa.archivist.domain

import com.zorroa.archivist.util.Json
import org.junit.Test
import kotlin.test.assertEquals

class ClipTests() {

    @Test
    fun testGeneratePileIdWithNoTimeline() {
        val doc = """
            {
                "source" : {
                  "path" : "/foo/bar.jpg",
                  "filesize" : 100
                },
                "media" : {
                  "timeCreated" : "2019-12-14T22:24:05.837301Z"
                }
              }
        """.trimIndent()
        val clip = Clip("page", 1f, 1f)
        val asset = Asset("abc123", Json.Mapper.readValue(doc, Json.GENERIC_MAP))
        val pileId = clip.putInPile(asset.id)
        assertEquals("vsUEh_BWG35e4H6pZMC0h9LKjwI", pileId)
    }

    @Test
    fun testGeneratePileIdWithTimeline() {
        val doc = """
            {
                "source" : {
                  "path" : "/foo/bar.jpg",
                  "filesize" : 100
                },
                "media" : {
                  "timeCreated" : "2019-12-14T22:24:05.837301Z"
                }
              }
        """.trimIndent()
        val clip = Clip("page", 1f, 1f, "pages")
        val asset = Asset("abc123", Json.Mapper.readValue(doc, Json.GENERIC_MAP))
        val pileId = clip.putInPile(asset.id)
        assertEquals("CtMU6JAcYwmAMi9lZ1Apth40fjw", pileId)
    }

    @Test
    fun testClipLength() {
        val clip = Clip("scene", 1023.23f, 1056.86f)
        assertEquals(33.64f, clip.length)
        assertEquals(1023.23f, clip.start)
        assertEquals(1056.86f, clip.stop)
    }

    @Test(expected = IllegalStateException::class)
    fun testIllegalClip() {
        Clip("scene", 1023.23f, 1f)

    }
}