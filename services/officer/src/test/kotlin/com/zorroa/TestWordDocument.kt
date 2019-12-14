package com.zorroa

import org.junit.Before
import org.junit.Test
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals

class TestWordDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/word_test.docx")
        opts.page = 1
        opts.outputDir = "docx"
    }

    @Test
    fun testRenderAll() {
        opts.page = -1

        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        validateMetadata(doc.getMetadata(1))
    }

    @Test
    fun testRender() {
        val opts = Options("src/test/resources/lighthouse.docx")
        opts.page = 1

        val doc = WordDocument(opts, FileInputStream(opts.fileName))
        doc.render()

        val image = ImageIO.read(doc.getImage(1))
        assertEquals(816, image.width)
        assertEquals(1056, image.height)

        validateMetadata(doc.getMetadata(1))
    }
}
