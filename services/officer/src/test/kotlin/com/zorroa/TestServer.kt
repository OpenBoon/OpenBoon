package com.zorroa

import com.github.kevinsawicki.http.HttpRequest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import java.io.File
import kotlin.test.assertEquals
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import spark.kotlin.stop

class TestServer {

    val storageClient = MinioStorageClient()

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
        val rsp = HttpRequest.get("http://localhost:9876/monitor/health")
        assertEquals(rsp.code(), 200)
        assertEquals("{\"status\": \"UP\"}", rsp.body())
    }

    @Test
    fun testStatusFailure() {
        val minioSpy = spy(storageClient.minioClient)
        doReturn(false).whenever(minioSpy).bucketExists(any())

        val rsp = HttpRequest.get("http://localhost:9876/monitor/health")
        assertEquals(rsp.code(), 200)
        assertEquals("{\"status\": \"UP\"}", rsp.body())
    }

    @Test
    fun testExistsFailure() {
        val opts = ExistsRequest(19, "foo")

        val rsp = HttpRequest.post("http://localhost:9876/exists")
            .send(Json.mapper.writeValueAsString(opts))
        assertEquals(404, rsp.code())
    }

    @Test
    fun testRender() {
        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"
        val rsp = HttpRequest.post("http://localhost:9876/render")
            .part("file", "CPB7_WEB.pdf", File("src/test/resources/CPB7_WEB.pdf"))
            .part("body", Json.mapper.writeValueAsString(opts))

        assert(rsp.code() == 201)

        val content = Json.mapper.readValue(rsp.body(), Map::class.java)
        assertEquals("render_test", content["location"])

        val exists = HttpRequest.post("http://localhost:9876/exists")
            .send(Json.mapper.writeValueAsString(opts))
        assertEquals(200, exists.code())
    }

    @Test
    fun testServerFailure() {
        val opts = RenderRequest("src/test/resources/boom.pdf")
        val rsp = HttpRequest.post("http://localhost:9876/render")
            .send(Json.mapper.writeValueAsString(opts))
            .code()
        assertEquals(rsp, 500)
    }
}
