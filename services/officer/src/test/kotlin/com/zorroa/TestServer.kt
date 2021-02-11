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
import org.junit.Ignore
import org.junit.Test
import spark.kotlin.stop
import kotlin.test.assertNotNull

@Ignore
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
        assertEquals(410, rsp.code())
    }

    @Test
    @Ignore
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

        var exists: HttpRequest? = null
        while (true) {
            // Wait Assync rendering
            exists = HttpRequest.post("http://localhost:9876/exists")
                .send(Json.mapper.writeValueAsString(opts))
            if (exists.code() != 404)
                break
            Thread.sleep(2000)
        }

        val existsRequest =
            ExistsRequest(page = opts.page, outputPath = opts.outputPath, requestId = content["request-id"].toString())

        assertEquals(200, exists?.code())
        assertEquals(false, WorkQueue.existsResquest(existsRequest))
    }

    @Test
    fun testServerFailure() {
        val opts = RenderRequest("src/test/resources/boom.pdf")
        val rsp = HttpRequest.post("http://localhost:9876/render")
            .send(Json.mapper.writeValueAsString(opts))
            .code()
        assertEquals(rsp, 500)
    }

    @Test
    fun testRepeatedRequest() {
        WorkQueue.redis.pool.purge()

        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"
        opts.requestId = "testId"

        for (i in 0..10) {
            postDocument(opts).code()
        }

        assert(WorkQueue.redis.pool.activeCount < 10)

        WorkQueue.unregisterRequest(opts)
        WorkQueue.redis.pool.purge()
    }

    @Test
    fun testUnregister() {
        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"
        opts.requestId = "testId"

        val rsp = postDocument(opts).code()
        assertNotNull(WorkQueue.redis.redisson?.getBucket<String>("testId"))
        assertEquals(true, WorkQueue.unregisterRequest(opts))
        assertEquals(false, WorkQueue.unregisterRequest(opts))
        assertEquals(201, rsp)
    }

    private fun postDocument(opts: RenderRequest): HttpRequest {
        return HttpRequest.post("http://localhost:9876/render")
            .part(
                "file",
                "CPB7_WEB.pdf",
                File("src/test/resources/CPB7_WEB.pdf")
            )
            .part("body", Json.mapper.writeValueAsString(opts))
    }
}
