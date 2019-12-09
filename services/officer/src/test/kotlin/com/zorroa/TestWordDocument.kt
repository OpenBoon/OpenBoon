package com.zorroa

import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestWordDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/word_test.docx")
        opts.page = 1
        opts.outputDir = "docx"
    }

    @Test
    fun testRenderAllPages() {
        val opts = Options("src/test/resources/word_test_2.docx")
        opts.outputDir = "all_docx"

        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.renderAllImages()

        for (page in 1..3) {
            doc.getImage(page)
        }
    }

    @Test
    fun testRender() {
        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderPage() {
        val opts = Options("src/test/resources/word_test_2.docx")
        opts.content = true
        opts.page = 0
        opts.outputDir = "render_word_page"
        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)

        assertTrue(metadata.containsKey("content"))
        println(metadata["content"])
    }
}
