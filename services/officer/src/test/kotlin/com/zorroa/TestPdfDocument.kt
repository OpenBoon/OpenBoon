package com.zorroa

import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Before

class TestPdfDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/CPB7_WEB.pdf")
        opts.outputDir = "pdf"
    }

    @Test
    fun testRenderAssetImage() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(0)

        val image = ImageIO.read(doc.getImage(0))
        assertEquals(637, image.width)
        assertEquals(825, image.height)
    }

    @Test
    fun testRenderAssetMetadata() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(0)
        validateAssetMetadata(doc.getMetadata(0))
    }

    @Test
    fun testRenderPageImage() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(1)

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(637, image.width)
        assertEquals(825, image.height)
    }

    @Test
    fun testRenderPageMetadata() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(1)

        validatePageMetadata(doc.getMetadata(1))
    }

    @Test
    fun testRenderAllImages() {
        val opts = Options("src/test/resources/pdf_test.pdf")
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        assertEquals(4, doc.renderAllImages())
    }
}
