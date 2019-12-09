package com.zorroa

import org.junit.Test
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class TestVisioDocument {

    @Test
    fun renderPageImage() {
        val opts = Options("src/test/resources/visio_test.vsdx")
        opts.page = 1

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(1)

        // validate we can load the page
        val page = doc.getImage(1)
        val image = ImageIO.read(page)
        assertEquals(1508, image.width)
        assertEquals(2136, image.height)
    }

    @Test
    fun renderAssetImage() {
        val opts = Options("src/test/resources/visio_test.vsdx")
        opts.page = 0

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(opts.page)

        // validate we can load the page
        val page = doc.getImage(0)
        val image = ImageIO.read(page)
        assertEquals(1508, image.width)
        assertEquals(2136, image.height)
    }

    @Test
    fun renderAssetMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(0)
        validateAssetMetadata(doc.getMetadata(0))
    }

    @Test
    fun renderPageMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(1)
        validatePageMetadata(doc.getMetadata(1))
    }

    @Test
    fun renderAllImages() {
        val opts = Options("src/test/resources/HRFLow.vsd")

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        assertEquals(5, doc.renderAllImages())
    }

    @Test
    fun renderAllMetadata() {
        val opts = Options("src/test/resources/HRFLow.vsd")
        opts.content = true

        val doc = VisioDocument(opts, FileInputStream(opts.fileName))
        assertEquals(5, doc.renderAllMetadata())
    }
}