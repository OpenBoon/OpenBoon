package com.zorroa.archivist.schema

import com.zorroa.archivist.domain.Document
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class DocumentTests {

    @Test
    fun IsEmpty() {
        val doc = Document()
        doc.setAttr("media", mutableMapOf<String, Any>())
        assertTrue(doc.isEmpty("media"))
    }

    @Test
    fun addToAttrUnqiue() {
        val doc = Document()
        assertTrue(doc.addToAttr("bing", "bong"))
        assertTrue(doc.addToAttr("bing", "bong", true))
        assertEquals(listOf("bong"), doc.getAttr("bing", List::class.java))
    }

    @Test
    fun addToAttr() {
        val doc = Document()
        assertTrue(doc.addToAttr("bing", "bong"))
        assertTrue(doc.addToAttr("bing", "bong", unique = false))
        assertEquals(listOf("bong", "bong"), doc.getAttr("bing", List::class.java))
    }

    @Test
    fun addToAttrCollection() {
        val doc = Document()
        assertTrue(doc.addToAttr("bing", listOf("spock", "kirk")))
        assertTrue(doc.addToAttr("bing", "bones"))
        assertEquals(listOf("spock", "kirk", "bones"), doc.getAttr("bing", List::class.java))
    }


    @Test
    fun removeFromAttr() {
        val doc = Document()
        doc.addToAttr("bing", listOf("spock", "kirk"))
        doc.removeFromAttr("bing", "spock")
        assertEquals(listOf("kirk"), doc.getAttr("bing", List::class.java))

    }
}