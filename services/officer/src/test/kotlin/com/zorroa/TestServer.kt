package com.zorroa

import com.github.kevinsawicki.http.HttpRequest
import org.apache.commons.io.FileUtils
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import spark.kotlin.stop
import java.io.File
import java.nio.file.Paths
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestServer {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            runServer(9876)
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            stop()
        }
    }

    @Before
    fun setup() {
        FileUtils.deleteDirectory(Paths.get(ServerOptions.storagePath).toFile())
    }

    @Test
    fun testOcrEndpoint() {
        val opts = Options("src/test/resources/ocr_test1.tif")
        opts.page = 1

        val rsp = HttpRequest.post("http://localhost:9876/ocr")
            .send(Json.mapper.writeValueAsString(opts))

        val body = Json.mapper.readValue(rsp.body(), Map::class.java)
        val metadata = Json.mapper.readValue(File(body["output"].toString() + "/metadata.1.json"), Map::class.java)
        assertTrue(metadata.containsKey("content"))
    }

    @Test
    fun testServer() {
        val opts = Options("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1

        val rsp = HttpRequest.post("http://localhost:9876/extract")
            .send(Json.mapper.writeValueAsString(opts))

        val body = Json.mapper.readValue(rsp.body(), Map::class.java)
        val image = ImageIO.read(File(body["output"].toString() + "/proxy.1.jpg"))
        assertEquals(637, image.width)
        assertEquals(825, image.height)

        val metadata = Json.mapper.readValue(
            File(body["output"].toString() + "/metadata.1.json"),
            Map::class.java
        )
        assertEquals("portrait", metadata["orientation"].toString())
    }

    @Test
    fun testServerFailure() {
        val opts = Options("src/test/resources/boom.pdf")
        val rsp = HttpRequest.post("http://localhost:9876/extract")
            .send(Json.mapper.writeValueAsString(opts))
            .code()
        assertEquals(rsp, 500)
    }
}
