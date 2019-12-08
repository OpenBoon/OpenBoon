package com.zorroa

import org.junit.Test
import java.io.FileInputStream
import kotlin.test.assertEquals

class TestVisioDocument {

    @Test
    fun renderImage() {
        val opts = Options("src/test/resources/visio_test.vsdx")
        opts.page = 1

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(1)
        doc.getImage(1)
    }

    @Test
    fun renderAllImages() {
        val opts = Options("src/test/resources/HRFLow.vsd")

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderAllImages()

        for (page in 1..4) {
            doc.getImage(page)
        }
    }

    @Test
    fun renderMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")
        opts.content = true

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(1)

        val metadata = Json.mapper.readValue(doc.getMetadata(1), Map::class.java)
        assertEquals("Eric Scott", metadata["author"])
        assertEquals("New Temp Employee", metadata["pageName"])
        assertEquals(4, metadata["pages"])
        assertEquals("landscape", metadata["orientation"])
    }

    @Test
    fun renderAllMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")
        opts.content = true

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderAllMetadata()

        val metadata = Json.mapper.readValue(doc.getMetadata(1), Map::class.java)
        assertEquals("Eric Scott", metadata["author"])
        assertEquals("New Temp Employee", metadata["pageName"])
        assertEquals(4, metadata["pages"])
        assertEquals("landscape", metadata["orientation"])
    }
}