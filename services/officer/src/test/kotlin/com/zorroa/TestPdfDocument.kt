package com.zorroa

import org.junit.Before
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TestPdfDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputDir = "pdf"
    }

    @Test
    fun testRender() {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(637, image.width)
        assertEquals(825, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadata(opts.page), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderAllImages() {
        val opts = Options("src/test/resources/pdf_test.pdf")

        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.renderAllImages()

        for (page in 1..3) {
            doc.getImage(page)
        }
    }

    @Test
    fun testSkipExistingFile() {
        val opts = Options("src/test/resources/pdf_test.pdf")
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        // Now render a page
        opts.page = 1
        doc.render()

        // checked logs manually.  If this was failing
        // a lot of other things would fail
    }

    @Test
    fun testRenderWithContent() {
        opts.content = true

        val metadata = extractPdfDocumentMetadata(opts)
        assertTrue(metadata.containsKey("content"))
        assertTrue(metadata.containsKey("content").toString().isNotBlank())

        opts.page = 1
        val metadata2 = extractPdfDocumentMetadata(opts)
        assertTrue(metadata.containsKey("content"))
        assertEquals(metadata["content"].toString(), metadata2["content"].toString())

        opts.page = 3
        val metadata3 = extractPdfDocumentMetadata(opts)
        assertTrue(metadata.containsKey("content"))
        assertNotEquals(metadata["content"].toString(), metadata3["content"].toString())
    }

    @Test
    fun testRenderHighDPI() {
        opts.dpi = 150

        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(1275, image.width)
        assertEquals(1650, image.height)
    }

    @Test
    fun testRenderPageWithContent() {
        opts.page = 3
        opts.content = true

        val metadata = extractPdfDocumentMetadata(opts)
        val content = metadata["content"].toString()
        assertTrue("Preface" in content)
    }

    private fun extractPdfDocumentMetadata(opts: Options): Map<*, *> {
        val doc = PdfDocument(opts, FileInputStream(opts.fileName))
        doc.render()
        return Json.mapper.readValue(doc.getMetadata(opts.page), Map::class.java)
    }
}
