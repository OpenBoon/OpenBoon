package com.zorroa

import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.Before
import org.junit.Test

class TestWordDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/word_test.docx")
        opts.page = 1
        opts.outputDir = "docx"
    }

    @Test
    fun testRenderPage() {
        val opts = Options("src/test/resources/word_test_2.docx")
        opts.page = 1

        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()
        doc.getImage(1)
    }

    @Test
    fun testRenderAll() {
        opts.page = -1

        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderAssetPage() {
        val opts = Options("src/test/resources/lighthouse.docx")
        opts.page = 0
        opts.outputDir = "render_asset_page"

        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        validateAssetMetadata(doc.getMetadata(page = 0))
    }
}
