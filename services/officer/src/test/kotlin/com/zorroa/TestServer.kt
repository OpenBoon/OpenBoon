package com.zorroa

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Ignore
class TestServer {

    val serverPort = 9876
    val basePath = "http://localhost:$serverPort"
    val webSocketClient = HttpClient() {
        install(io.ktor.client.features.websocket.WebSockets)
    }

    companion object {
        private var engine: ApplicationEngine? = null

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("ZMLP_STORAGE_CLIENT", "minio")
            System.setProperty("REDIS_HOST", "localhost:6379")
            engine = runKtorServer(9876)
            Thread.sleep(5000)
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            engine?.stop(1L, 1L)
        }
    }

    @Test
    fun testStatus() {
        runBlocking {
            val get = webSocketClient.get<HttpResponse> {
                url(Url("$basePath/monitor/health"))
            }

            assertEquals(get.content.readUTF8Line(), "{\"status\": \"UP\"}")
            assertEquals(get.status, HttpStatusCode.OK)
        }
    }

    @Test
    fun testStatusFailure() {
        withTestApplication {
            application.install(Routing) {
                get("/monitor/health") {
                    call.respond(HttpStatusCode.OK, "{\"status\": \"DOWN\"}")
                }
            }
            with(handleRequest(HttpMethod.Get, "/monitor/health")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"status\": \"DOWN\"}", response.content)
            }
        }
    }

    @Test
    fun testExistsFailure() {

        runBlocking {
            webSocketClient.ws(method = HttpMethod.Get, host = "localhost", port = serverPort, path = "/exists") {
                val opts = ExistsRequest(19, "foo")
                send(Frame.Text(Json.mapper.writeValueAsString(opts)))

                val receive = incoming.receive() as Frame.Text
                val response = Json.mapper.readValue(receive.readText(), Map::class.java)

                assertEquals(ExistsStatus.NOT_EXISTS.toString(), response["status"])
                assertEquals("foo", response["location"])
            }
        }
    }

    @Test
    @Ignore
    fun testRender() {
        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"

        runBlocking {
            webSocketClient.ws(host = "localhost", port = serverPort, path = "/render") {
                val content = prepareRenderRequest(opts, "src/test/resources/CPB7_WEB.pdf")

                send(Frame.Text(Json.mapper.writeValueAsString(content)))

                val response = responseAsMap(incoming.receive())

                assertEquals(RenderStatus.RENDER_QUEUE.toString(), response["status"])
                assertEquals(712, response["page-count"])
                assertEquals("render_test", response["location"])
            }

            webSocketClient.ws(host = "localhost", port = serverPort, path = "/exists") {

                while (true) {
                    send(Frame.Text(Json.mapper.writeValueAsString(opts)))

                    val receive = responseAsMap(incoming.receive())
                    if (receive["status"] != ExistsStatus.RENDERING.toString()) {
                        close()
                        assertEquals(ExistsStatus.EXISTS.toString(), receive["status"])
                        assertEquals("render_test", receive["location"])
                        break
                    }
                    Thread.sleep(2000)
                }

                val existsRequest =
                    ExistsRequest(page = opts.page, outputPath = opts.outputPath, requestId = opts.requestId)

                assertEquals(false, WorkQueue.existsResquest(existsRequest))
            }
        }
    }

    @Test
    fun testServerBadRequestFailure() {
        val opts = RenderRequest("src/test/resources/boom.pdf")

        runBlocking {
            webSocketClient.ws(host = "localhost", port = serverPort, path = "/render") {
                send(Frame.Text(Json.mapper.writeValueAsString(opts)))

                val responseAsMap = responseAsMap(incoming.receive())
                assertEquals(RenderStatus.BAD_REQUEST.toString(), responseAsMap["status"])
            }
        }
    }

    @Test
    fun testServerFailure() {
        val opts = RenderRequest("src/test/resources/proxy.0.jpg")
        val request = prepareRenderRequest(opts, "src/test/resources/proxy.0.jpg")

        runBlocking {
            webSocketClient.ws(host = "localhost", port = serverPort, path = "/render") {
                send(Frame.Text(Json.mapper.writeValueAsString(request)))

                val responseAsMap = responseAsMap(incoming.receive())
                assertEquals(RenderStatus.FAIL.toString(), responseAsMap["status"])
            }
        }
    }

    @Test
    fun testRepeatedRequest() {
        WorkQueue.redis.pool.purge()

        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"
        opts.requestId = "testId"

        val request = prepareRenderRequest(opts, "src/test/resources/CPB7_WEB.pdf")

        for (i in 0..10) {
            runBlocking {
                webSocketClient.ws(host = "localhost", port = serverPort, path = "/render") {
                    send(Frame.Text(Json.mapper.writeValueAsString(request)))
                }
            }
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

        val request = prepareRenderRequest(opts, "src/test/resources/CPB7_WEB.pdf")

        runBlocking {
            webSocketClient.ws(host = "localhost", port = serverPort, path = "/render") {
                send(Frame.Text(Json.mapper.writeValueAsString(request)))
                val responseAsMap = responseAsMap(incoming.receive())

                assertNotNull(WorkQueue.redis.redisson?.getBucket<String>("testId"))
                assertEquals(true, WorkQueue.unregisterRequest(opts))
                assertEquals(false, WorkQueue.unregisterRequest(opts))
                assertEquals(RenderStatus.RENDER_QUEUE.toString(), responseAsMap["status"])
            }
        }
    }

    private fun prepareRenderRequest(opts: RenderRequest, filePath: String): Map<String, String> {
        return mapOf<String, String>(
            "file" to Base64.getEncoder().encodeToString(File(filePath).readBytes()),
            "body" to Json.mapper.writeValueAsString(opts)
        )
    }

    private fun responseAsMap(frame: Frame): Map<*, *> {
        val response = (frame as Frame.Text).readText()
        return Json.mapper.readValue(response, Map::class.java)
    }
}
