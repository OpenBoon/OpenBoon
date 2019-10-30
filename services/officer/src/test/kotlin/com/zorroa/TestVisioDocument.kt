package com.zorroa

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestVisioDocument {

    @Test
    fun renderImage() {
        val opts = Options("src/test/resources/visio_test.vsdx")
        opts.page = 1

        val doc = VisioDocument(opts)
        doc.renderImage(1)

        val files = doc.ioHandler.outputRoot.toFile().listFiles()
        assertEquals(1, files.size)
    }

    @Test
    fun renderAllImages() {
        val opts = Options("src/test/resources/HRFLow.vsd")

        val doc = VisioDocument(opts)
        doc.renderAllImages()

        val files = doc.ioHandler.outputRoot.toFile().listFiles().toSet().map { it.name }
        assertEquals(4, files.size)
        for (page in 1..4) {
            assertTrue("proxy.$page.jpg" in files)
        }
    }

    @Test
    fun renderMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")
        opts.content = true

        val doc = VisioDocument(opts)
        doc.renderMetadata(1)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(1), Map::class.java)
        assertEquals("Eric Scott", metadata["author"])
        assertEquals("New Temp Employee", metadata["pageName"])
        assertEquals(4, metadata["pages"])
        assertEquals("landscape", metadata["orientation"])
    }

    @Test
    fun renderAllMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")
        opts.content = true

        val doc = VisioDocument(opts)
        doc.renderAllMetadata()

        val metadata = Json.mapper.readValue(doc.getMetadataFile(1), Map::class.java)
        assertEquals("Eric Scott", metadata["author"])
        assertEquals("New Temp Employee", metadata["pageName"])
        assertEquals(4, metadata["pages"])
        assertEquals("landscape", metadata["orientation"])
    }
}