package com.zorroa

import org.apache.commons.io.FileUtils
import org.junit.Before
import java.nio.file.Paths
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
        FileUtils.deleteDirectory(Paths.get(ServerOptions.storagePath).toFile())
    }

    @Test
    fun testRender() {
        val doc = PdfDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(637, image.width)
        assertEquals(825, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(opts.page), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderPageFromGCS() {
        val opts = Options("gs://zorroa-dev-data/office/pdfTest.pdf")
        opts.page = 1
        val doc = PdfDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(1066, image.width)
        assertEquals(800, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(opts.page), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderAllImages() {
        val opts = Options("src/test/resources/pdf_test.pdf")

        val doc = PdfDocument(opts)
        doc.renderAllImages()

        val files = doc.ioHandler.outputRoot.toFile().listFiles().toSet().map { it.name }
        assertEquals(3, files.size)
        for (page in 1..3) {
            assertTrue("proxy.$page.jpg" in files)
        }
    }

    @Test
    fun testSkipExistingFile() {
        val opts = Options("src/test/resources/pdf_test.pdf")
        val doc = PdfDocument(opts)
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

        val doc = PdfDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(1275, image.width)
        assertEquals(1650, image.height)
    }

    @Test
    fun testRenderPageWithContent() {
        opts.page = 3
        opts.content = true

        val metadata = extractPdfDocumentMetadata(opts)
        val content = metadata["content"].toString()
        println(content)
        assertTrue("Preface" in content)
    }

    private fun extractPdfDocumentMetadata(opts: Options): Map<*, *> {
        val doc = PdfDocument(opts)
        doc.render()
        return Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)
    }
}
