package com.zorroa

import com.fasterxml.jackson.module.kotlin.readValue
import io.minio.errors.ErrorResponseException
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class TestIOHandler {

    val options = RenderRequest("foo.docx")
    val handler = IOHandler(options)

    @Test
    fun getWriteAndGetMetadata() {
        val out = ReversibleByteArrayOutputStream()
        out.write("{\"author\": \"kirk\"}".toByteArray())
        handler.writeMetadata(5, out)

        val metadata = handler.getMetadata(5)
        val map = Json.mapper.readValue<Map<String, Any>>(metadata)
        assertEquals("kirk", map["author"])
    }

    @Test
    fun getWriteAndGetImage() {
        val out = ReversibleByteArrayOutputStream()
        out.write(FileInputStream(File("src/test/resources/proxy.0.jpg")).readAllBytes())
        handler.writeImage(1, out)

        val bytes = handler.getImage(1)
        val image = ImageIO.read(bytes)

        assertEquals(816, image.width)
        assertEquals(1056, image.height)
    }

    @Test(expected = ErrorResponseException::class)
    fun getMetadataFailure() {
        handler.getMetadata(100)
    }

    @Test(expected = ErrorResponseException::class)
    fun getImageFailure() {
        handler.getImage(100)
    }

    @Test
    fun testExists() {
        assertFalse(handler.exists(1))
        val out = ReversibleByteArrayOutputStream()
        out.write("{\"author\": \"jim\"}".toByteArray())
        handler.writeMetadata(1, out)
        assertTrue(handler.exists(1))
    }

    @Test
    fun testGetImagePath() {
        val path = handler.getImagePath(5)
        assertEquals("${options.outputPath}_proxy.5.jpg", path)
    }

    @Test
    fun getMetadataPath() {
        val path = handler.getMetadataPath(5)
        assertEquals("${options.outputPath}_metadata.5.json", path)
    }

    @Test
    fun getOutputUri() {
        val path = handler.getOutputPath()
        assertEquals(options.outputPath, path)
    }
}
