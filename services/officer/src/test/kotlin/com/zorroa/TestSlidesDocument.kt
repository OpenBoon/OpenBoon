package com.zorroa

import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class TestSlidesDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/pptx_test.pptx")
        opts.outputDir = "pptx"
    }

    @Test
    fun testRenderAsset() {
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(0))
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        validateAssetMetadata(doc.getMetadata(0))
    }

    @Test
    fun testRenderPage() {
        opts.content = true
        opts.page = 1
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        validatePageMetadata(doc.getMetadata(1))
    }

    @Test
    fun testRenderAll() {
        val opts = Options("src/test/resources/pptx_test.pptx")
        val doc = SlidesDocument(opts, FileInputStream(opts.fileName))
        assertEquals(4, doc.renderAllImages())
    }
}
