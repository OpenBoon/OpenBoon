package com.zorroa

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readText
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.Base64
import java.util.UUID
import kotlin.system.exitProcess

const val ASPOSE_LICENSE_FILE = "Aspose.Total.Java.lic"

/**
 * A render request
 */
class RenderRequest(
    val fileName: String,
    var page: Int = -1,
    var outputPath: String = "/projects/" + UUID.randomUUID().toString(),
    var dpi: Int = 100,
    var disableImageRender: Boolean = false,
    var requestId: String = UUID.randomUUID().toString(),
)

/**
 * Exists request.
 */
class ExistsRequest(
    val page: Int,
    val outputPath: String,
    val requestId: String? = null,
)

/**
 * Extract the image and metadata to their resting place.
 */
fun extract(opts: RenderRequest, input: InputStream): Document {
    requireNotNull(opts.outputPath) { "An output directory must be provided" }

    val fileExt = opts.fileName.substringAfterLast('.', "").toLowerCase()

    input.use { file ->
        val doc = when (fileExt) {
            "pdf" -> PdfDocument(opts, file)
            "ppt", "pptx", "odp" -> SlidesDocument(opts, file)
            "doc", "docx" -> WordDocument(opts, file)
            "xls", "xlsx" -> CellsDocument(opts, file)
            "vsd", "vsdx" -> VisioDocument(opts, file)
            else -> {
                val msg = "Invalid inputFile type: $fileExt"
                println(msg)
                throw Exception(msg)
            }
        }

        return doc
    }
}

fun backoffResponse(): MutableMap<String, Any>? {
    val os = ManagementFactory.getOperatingSystemMXBean()
    val maxLoad = os.availableProcessors * Config.officer.loadMultiplier
    return if (os.systemLoadAverage > maxLoad) {
        mutableMapOf(
            "load" to os.systemLoadAverage,
            "max" to os.availableProcessors * Config.officer.loadMultiplier
        )
    } else {
        null
    }
}

/**
 * Start a server to handle multiple requests.
 */
fun runKtorServer(port: Int): ApplicationEngine {

    val logger: Logger = LoggerFactory.getLogger("com.zorroa.officer")
    logger.info("starting server!")

    // Init the storage manager
    StorageManager
    WorkQueue

    return embeddedServer(
        Netty,
        port = port,
    ) {
        install(WebSockets)
        install(CallLogging)

        logger.info("init web server: port=$port")

        routing {
            get("/monitor/health") {
                if (StorageManager.storageClient().bucketExists(Config.bucket.name)) {
                    call.respond("""{"status": "UP"}""")
                } else {
                    call.respond("""{"status": "DOWN"}""")
                }
            }

            webSocket("/exists") {
                incoming.consumeEach {
                    val frame = it as Frame.Text
                    val response = mutableMapOf<String, Any>()
                    val options = Json.mapper.readValue<ExistsRequest>(frame.readText())
                    val ioHandler = IOHandler(RenderRequest("none", options.page, options.outputPath))

                    logger.info("checking output path: {}", options.outputPath)

                    if (ioHandler.exists(options.page) && !WorkQueue.existsResquest(options)) {
                        response["status"] = ExistsStatus.EXISTS
                    } else if (WorkQueue.existsResquest(options)) {
                        // Waiting for rendering
                        response["status"] = ExistsStatus.RENDERING
                    } else {
                        // Don't exists and is not in rendering queue
                        response["status"] = ExistsStatus.NOT_EXISTS
                    }
                    response["location"] = ioHandler.getOutputPath()
                    outgoing.send(Frame.Text(Json.mapper.writeValueAsString(response)))
                }
            }

            webSocket("/render") {
                incoming.receive().let {

                    try {
                        val request = it as Frame.Text
                        val content = Json.mapper.readValue(request.readText(), Map::class.java)
                        val base64File = content["file"].toString()

                        val file = Base64.getDecoder().decode(base64File) as ByteArray
                        val body = content["body"] as String

                        val backoff = backoffResponse()

                        if (backoff != null) {
                            backoff["status"] = RenderStatus.TOO_MANY_REQUESTS
                            outgoing.send(Frame.Text(Json.mapper.writeValueAsString(backoff)))
                        } else {

                            val req = Json.mapper.readValue<RenderRequest>(body)
                            val doc = extract(req, file.inputStream())

                            val response = Json.mapper.writeValueAsString(
                                mapOf(
                                    "location" to doc.ioHandler.getOutputPath(),
                                    "page-count" to doc.pageCount(),
                                    "request-id" to req.requestId,
                                    "status" to RenderStatus.RENDER_QUEUE
                                )
                            )
                            WorkQueue.execute(WorkQueueEntry(doc, req))
                            send(Frame.Text(response))
                        }
                    } catch (eNull: NullPointerException) {
                        logger.warn("Bad Request Exception")
                        send(
                            Frame.Text(
                                Json.mapper.writeValueAsString(
                                    mapOf(
                                        "status" to RenderStatus.BAD_REQUEST,
                                        "message" to eNull.message
                                    )
                                )
                            )
                        )
                    } catch (e: Exception) {
                        logger.warn("failed to process", e)
                        send(
                            Frame.Text(
                                Json.mapper.writeValueAsString(
                                    mapOf(
                                        "status" to RenderStatus.FAIL,
                                        "message" to e.message
                                    )
                                )
                            )
                        )
                    }
                }
            }
        }
    }.apply {
        start(wait = false)
    }
}

fun main() {
    try {
        Config.logSystemConfiguration()
        runKtorServer(Config.officer.port)
    } catch (e: Exception) {
        println(e.message)
        exitProcess(1)
    }
}
