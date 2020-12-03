package com.zorroa

import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Before

class TestPdfDocument {

    private lateinit var opts: RenderRequest

    @Before
    fun setup() {
        opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.outputPath = "pdf"
    }

    @Test
    fun testRenderPageImage() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderImage(1)

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(850, image.width)
        assertEquals(1100, image.height)
    }

    @Test
    fun testRenderPageMetadata() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderMetadata(1)
        validateMetadata(doc.getMetadata(1))
    }

    @Test
    fun testRenderAllImages() {
        val opts = RenderRequest("src/test/resources/pdf_test.pdf")
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        assertEquals(3, doc.renderAllImages())
    }
}
