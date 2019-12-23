package com.zorroa

import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class TestSlidesDocument {

    private lateinit var opts: RenderRequest

    @Before
    fun setup() {
        opts = RenderRequest("src/test/resources/pptx_test.pptx")
    }

    @Test
    fun testRenderPage() {
        opts.page = 1
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(1024, image.width)
        assertEquals(768, image.height)
        validateMetadata(doc.getMetadata(1))
    }

    @Test
    fun testRenderAll() {
        val opts = RenderRequest("src/test/resources/pptx_test.pptx")
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        assertEquals(3, doc.renderAllImages())
    }
}
