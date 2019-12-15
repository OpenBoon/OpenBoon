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
        val pileId = clip.generatePileId(asset)
        assertEquals("zrWlYyy9ji3_WoWn5oEQGxD4248", pileId)
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
        val pileId = clip.generatePileId(asset)
        assertEquals("JVazbqBuS2mprozn3ZGB6Ao6oDc", pileId)
    }

    @Test
    fun testGeneratePileIdWithNoTime() {
        val doc = """
            {
                "source" : {
                  "path" : "/foo/bar.jpg",
                  "filesize" : 100
                }
              }
        """.trimIndent()
        val clip = Clip("page", 1f, 1f, "pages")
        val asset = Asset("abc123", Json.Mapper.readValue(doc, Json.GENERIC_MAP))
        val pileId = clip.generatePileId(asset)
        assertEquals("2yZvYnWZoEjM0SszzuDvj49O2GE", pileId)
    }

    @Test
    fun testGeneratePileIdWithNoSize() {
        val doc = """
            {
                "source" : {
                  "path" : "/foo/bar.jpg"
                }
              }
        """.trimIndent()
        val clip = Clip("page", 1f, 1f, "pages")
        val asset = Asset("abc123", Json.Mapper.readValue(doc, Json.GENERIC_MAP))
        val pileId = clip.generatePileId(asset)
        assertEquals("iK9CAQlvboR1LXr3oqheFoWRhWM", pileId)
    }

    @Test
    fun testGeneratePileIdWithNoPath() {
        val clip = Clip("page", 1f, 1f, "pages")
        val asset = Asset("abc123", mapOf())
        val pileId = clip.generatePileId(asset)
        assertEquals("gIjnIgVCxeoDkrc6DrUzT_lA1ws", pileId)
    }
}