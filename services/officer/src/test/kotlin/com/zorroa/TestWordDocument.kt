package com.zorroa

import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test

class TestWordDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/word_test.docx")
        opts.page = 1
        opts.outputDir = "docx"
        FileUtils.deleteDirectory(Paths.get(ServerOptions.storagePath).toFile())
    }

    @Test
    fun testRenderAllPages() {
        val opts = Options("src/test/resources/word_test_2.docx")
        opts.outputDir = "all_docx"

        val doc = WordDocument(opts)
        doc.renderAllImages()

        val files = doc.getOutputRoot().toFile().listFiles().toSet().map { it.name }
        assertEquals(3, files.size)
        for (page in 1..3) {
            assertTrue("proxy.$page.jpg" in files)
        }
    }

    @Test
    fun testRender() {
        val doc = WordDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderPage() {
        opts.content = true
        opts.page = 1
        val doc = WordDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)

        assertTrue(metadata.containsKey("content"))
        println(metadata["content"])
    }

    @Test
    fun testRenderPageFromGcs() {
        val opts = Options("gs://zorroa-dev-data/office/MAPPING PRINTERS FOR THE BOSTON ONE FEDERAL STREET LOCATION.doc")
        opts.content = true
        opts.page = 1

        val doc = WordDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)

        assertTrue(metadata.containsKey("content"))
        println(metadata["content"])
    }
}
