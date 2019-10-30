package com.zorroa

import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test

class TestSlidesDocument {

    private lateinit var opts: Options

    @Before
    fun setup() {
        opts = Options("src/test/resources/pptx_test.pptx")
        opts.page = 1
        opts.outputDir = "pptx"
        FileUtils.deleteDirectory(Paths.get(ServerOptions.storagePath).toFile())
    }

    @Test
    fun testRender() {
        val doc = SlidesDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)
        assertFalse(metadata.containsKey("content"))
    }

    @Test
    fun testRenderPage() {
        opts.content = true
        opts.page = 1
        val doc = SlidesDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)
        assertTrue(metadata.containsKey("content"))
        println(metadata["content"])
    }

    @Test
    fun testRenderPageFromGCS() {
        val opts = Options("gs://zorroa-dev-data/office/pptx_test.pptx")
        opts.content = true
        opts.page = 2

        val doc = SlidesDocument(opts)
        doc.render()

        val image = ImageIO.read(doc.getImageFile())
        assertEquals(1024, image.width)
        assertEquals(768, image.height)

        val metadata = Json.mapper.readValue(doc.getMetadataFile(), Map::class.java)
        assertTrue(metadata.containsKey("content"))
        println(metadata["content"])
    }

    @Test
    fun testRenderAllImages() {
        val opts = Options("src/test/resources/pptx_test.pptx")

        val doc = SlidesDocument(opts)
        doc.renderAllImages()

        val files = doc.ioHandler.outputRoot.toFile().listFiles().toSet().map { it.name }
        assertEquals(3, files.size)
        for (page in 1..3) {
            assertTrue("proxy.$page.jpg" in files)
        }
    }
}
