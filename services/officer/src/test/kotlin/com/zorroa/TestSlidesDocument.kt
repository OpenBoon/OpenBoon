package com.zorroa

import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestSlidesDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/pptx_test.pptx")
        opts.page = 1
        opts.outputDir = "pptx"
    }

    @Test
    fun testRender() {
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderPage() {
        opts.content = true
        opts.page = 1
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage())
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadata(), Map::class.java)
        assertTrue(metadata.containsKey("content"))
        println(metadata["content"])
    }

    @Test
    fun testRenderAllImages() {
        val opts = Options("src/test/resources/pptx_test.pptx")
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        doc.renderAllImages()

        for (page in 1..3) {
            doc.getImage(page)
        }
    }
}
