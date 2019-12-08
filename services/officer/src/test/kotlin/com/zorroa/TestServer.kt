package com.zorroa

import com.github.kevinsawicki.http.HttpRequest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import spark.kotlin.stop
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestServer {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            println("start")
            runServer(9876)
            println("started")
            Thread.sleep(1000)
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            stop()
        }
    }

    @Test
    fun testStatus() {
        val rsp = HttpRequest.get("http://localhost:9876/status")
        assertEquals(rsp.code(), 200)
    }

    @Test
    fun testOcrEndpoint() {
        val opts = Options("src/test/resources/ocr_test1.tif")
        opts.page = 1
        opts.outputDir = "ocr_test"

        val rsp = HttpRequest.post("http://localhost:9876/ocr")
            .part("file", "ocr_test1.tif", File("src/test/resources/ocr_test1.tif"))
            .part("body", Json.mapper.writeValueAsString(opts))

        assert(rsp.code() == 201)

        val content = Json.mapper.readValue(rsp.body(), Map::class.java)
        assertEquals("pixml://ml-storage/officer/ocr_test", content["output"])
    }

    @Test
    fun testExtract() {
        val opts = Options("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputDir = "extract_test"
        val rsp = HttpRequest.post("http://localhost:9876/extract")
            .part("file", "CPB7_WEB.pdf", File("src/test/resources/CPB7_WEB.pdf"))
            .part("body", Json.mapper.writeValueAsString(opts))

        assert(rsp.code() == 201)

        val content = Json.mapper.readValue(rsp.body(), Map::class.java)
        assertEquals("pixml://ml-storage/officer/extract_test", content["output"])
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
