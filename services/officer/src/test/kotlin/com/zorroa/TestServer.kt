package com.zorroa

import com.github.kevinsawicki.http.HttpRequest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import spark.kotlin.stop
import java.io.File
import kotlin.test.assertEquals

class TestServer {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            runServer(9876)
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
    fun testExistsFailure() {
        val opts = Options("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1

        val rsp = HttpRequest.post("http://localhost:9876/exists").
            send(Json.mapper.writeValueAsString(opts))
        assertEquals(404, rsp.code())
    }

    @Test
    fun testRender() {
        val opts = Options("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputDir = "render_test"
        val rsp = HttpRequest.post("http://localhost:9876/render")
            .part("file", "CPB7_WEB.pdf", File("src/test/resources/CPB7_WEB.pdf"))
            .part("body", Json.mapper.writeValueAsString(opts))

        assert(rsp.code() == 201)

        val content = Json.mapper.readValue(rsp.body(), Map::class.java)
        val prefix = IOHandler.PREFIX
        assertEquals("pixml://ml-storage/$prefix/render_test", content["output"])

        val exists = HttpRequest.post("http://localhost:9876/exists").
            send(Json.mapper.writeValueAsString(opts))
        assertEquals(200, exists.code())
    }

    @Test
    fun testServerFailure() {
        val opts = Options("src/test/resources/boom.pdf")
        val rsp = HttpRequest.post("http://localhost:9876/render")
            .send(Json.mapper.writeValueAsString(opts))
            .code()
        assertEquals(rsp, 500)
    }
}
