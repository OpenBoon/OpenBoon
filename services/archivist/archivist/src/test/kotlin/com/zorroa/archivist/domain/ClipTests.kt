package com.zorroa.archivist.domain

import com.zorroa.archivist.util.bd
import com.zorroa.zmlp.util.Json
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class ClipTests() {

    @Test
    fun testGeneratePileIdWithNoTimeline() {
        val doc =
            """
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
        val clip = Clip("page", BigDecimal(1.0), BigDecimal(1.0))
        val asset = Asset("abc123", Json.Mapper.readValue(doc, Json.GENERIC_MAP).toMutableMap())
        val pileId = clip.putInPile(asset.id)
        assertEquals("vsUEh_BWG35e4H6pZMC0h9LKjwI", pileId)
    }

    @Test
    fun testGeneratePileIdWithTimeline() {
        val doc =
            """
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
        val clip = Clip("page", BigDecimal(1.0), BigDecimal(1.0), "pages")
        val asset = Asset("abc123", Json.Mapper.readValue(doc, Json.GENERIC_MAP).toMutableMap())
        val pileId = clip.putInPile(asset.id)
        assertEquals("CtMU6JAcYwmAMi9lZ1Apth40fjw", pileId)
    }

    @Test
    fun testClipLength() {
        val clip = Clip("scene", BigDecimal(1023.23), BigDecimal(1056.86))
        assertEquals(33.63.bd(), clip.length)
        assertEquals(1023.23.bd(), clip.start)
        assertEquals(1056.86.bd(), clip.stop)
    }

    @Test(expected = IllegalStateException::class)
    fun testIllegalClip() {
        Clip("scene", BigDecimal(1023.23), BigDecimal(1.0))
    }
}
