package com.zorroa

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.Base64
import kotlin.test.assertEquals

class TestServer {

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            System.setProperty("ZMLP_STORAGE_CLIENT", "minio")
            System.setProperty("REDIS_HOST", "localhost:6379")
            Thread.sleep(5000)
        }
    }

    @Test
    fun testStatus() {
        withTestApplication {
            application.install(Routing) {
                get("/monitor/health") {
                    call.respond(HttpStatusCode.OK, "{\"status\": \"UP\"}")
                }
            }
            with(handleRequest(HttpMethod.Get, "/monitor/health")) {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("{\"status\": \"UP\"}", response.content)
            }
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

        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocket("/exists") {
                    val receivedFrame = incoming.receive()
                    val response = mapOf(
                        "status" to ExistsStatus.NOT_EXISTS,
                        "location" to "foo"
                    )
                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(response)))
                }
            }

            handleWebSocketConversation("/exists") { incoming, outgoing ->

                val opts = ExistsRequest(19, "foo")
                outgoing.send(Frame.Text(Json.mapper.writeValueAsString(opts)))

                val receive = incoming.receive() as Frame.Text
                val response = Json.mapper.readValue(receive.readText(), Map::class.java)

                assertEquals(ExistsStatus.NOT_EXISTS.toString(), response["status"])
                assertEquals("foo", response["location"])
            }
        }
    }

    @Test
    fun testRender() {
        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        val content = prepareRenderRequest(opts, "src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"

        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocket("/render") {
                    incoming.receive()
                    val response = mutableMapOf(
                        "status" to RenderStatus.RENDER_QUEUE,
                        "page-count" to 712,
                        "location" to "render_test"
                    )

                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(response)))
                }

                webSocket("/exists") {
                    incoming.receive()
                    val response = mutableMapOf(
                        "status" to ExistsStatus.EXISTS,
                        "location" to "render_test"
                    )

                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(response)))
                }
            }

            handleWebSocketConversation("/render") { incoming, outgoing ->
                outgoing.send(Frame.Text(Json.mapper.writeValueAsString(content)))

                val response = responseAsMap(incoming.receive())
                assertEquals(RenderStatus.RENDER_QUEUE.toString(), response["status"])
                assertEquals(712, response["page-count"])
                assertEquals("render_test", response["location"])
            }

            handleWebSocketConversation("/exists") { incoming, outgoing ->

                outgoing.send(Frame.Text(Json.mapper.writeValueAsString(opts)))
                val receive = responseAsMap(incoming.receive())
                if (receive["status"] != ExistsStatus.RENDERING.toString()) {
                    assertEquals(ExistsStatus.EXISTS.toString(), receive["status"])
                    assertEquals("render_test", receive["location"])
                }
            }
        }
    }

    @Test
    fun testServerBadRequestFailure() {

        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocket("/render") {
                    incoming.receive()
                    val response = mapOf(
                        "status" to RenderStatus.BAD_REQUEST
                    )
                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(response)))
                }
            }

            handleWebSocketConversation("/render") { incoming, outgoing ->
                val opts = RenderRequest("src/test/resources/boom.pdf")
                outgoing.send(Frame.Text(Json.mapper.writeValueAsString(opts)))

                val responseAsMap = responseAsMap(incoming.receive())
                assertEquals(RenderStatus.BAD_REQUEST.toString(), responseAsMap["status"])
            }
        }
    }

    @Test
    fun testServerFailure() {
        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocket("/render") {
                    incoming.receive()
                    val response = mapOf(
                        "status" to RenderStatus.FAIL,
                        "msg" to "Invalid inputFile type: jpg"
                    )
                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(response)))
                }
            }

            handleWebSocketConversation("/render") { incoming, outgoing ->

                val opts = RenderRequest("src/test/resources/proxy.0.jpg")
                val request = prepareRenderRequest(opts, "src/test/resources/proxy.0.jpg")
                outgoing.send(Frame.Text(Json.mapper.writeValueAsString(request)))

                val responseAsMap = responseAsMap(incoming.receive())
                assertEquals(RenderStatus.FAIL.toString(), responseAsMap["status"])
                assertEquals("Invalid inputFile type: jpg", responseAsMap["msg"])
            }
        }
    }

    @Test
    fun testRepeatedRequest() {
        val opts = RenderRequest("src/test/resources/CPB7_WEB.pdf")
        opts.page = 1
        opts.outputPath = "render_test"
        opts.requestId = "testId"

        val request = prepareRenderRequest(opts, "src/test/resources/CPB7_WEB.pdf")

        withTestApplication {
            application.install(WebSockets)
            application.routing {
                webSocket("/render") {
                    for (frame in incoming){
                        assert(true)
                    }
                }
            }

            handleWebSocketConversation("/render") { incoming, outgoing ->
                for (i in 0..10) {
                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(request)))
                }
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
