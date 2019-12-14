package com.zorroa

import org.junit.Ignore
import org.junit.Test
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class TestVisioDocument {

    @Test
    @Ignore
    fun renderPageImage() {
        val opts = RenderRequest("src/test/resources/visio_test.vsdx")
        opts.page = 1

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(1)

        // validate we can load the page
        val inputStream = doc.getImage(1)
        val image = ImageIO.read(inputStream)
        assertEquals(1508, image.width)
        assertEquals(2136, image.height)
    }

    @Test
    fun renderPageMetadata() {
        val opts = RenderRequest("src/test/resources/HRFLow.vsd")

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(1)
        validateMetadata(doc.getMetadata(1))
    }

    @Test
    fun renderAllImages() {
        val opts = RenderRequest("src/test/resources/HRFLow.vsd")
        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        assertEquals(4, doc.renderAllImages())
    }

    @Test
    fun renderAllMetadata() {
        val opts = RenderRequest("src/test/resources/HRFLow.vsd")
        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        assertEquals(4, doc.renderAllMetadata())
    }
}
