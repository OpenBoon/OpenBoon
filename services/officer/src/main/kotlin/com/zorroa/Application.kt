package com.zorroa

import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.kotlin.get
import spark.kotlin.post
import spark.kotlin.threadPool
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.UUID
import javax.servlet.MultipartConfigElement
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
    var requestId: String = UUID.randomUUID().toString()
)

/**
 * Exists request.
 */
class ExistsRequest(
    val page: Int,
    val outputPath: String
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

fun backoffResponse(): Map<String, Any>? {
    val os = ManagementFactory.getOperatingSystemMXBean()
    val maxLoad = os.availableProcessors * Config.officer.loadMultiplier
    return if (os.systemLoadAverage > maxLoad) {
        mapOf(
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
fun runServer(port: Int) {

    val logger: Logger = LoggerFactory.getLogger("com.zorroa.officer")
    val os = ManagementFactory.getOperatingSystemMXBean()

    logger.info("starting server!")

    // Init the storage manager
    StorageManager
    WorkQueue

    val threads = (os.availableProcessors * 3).coerceAtLeast(8)
    logger.info("init web server: threads=$threads port=$port")

    spark.kotlin.port(port)
    threadPool(threads, threads, 600 * 1000)

    post("/exists", "application/json") {
        val options = Json.mapper.readValue<ExistsRequest>(this.request.body())
        val ioHandler = IOHandler(RenderRequest("none", options.page, options.outputPath))
        logger.info("checking output path: {}", options.outputPath)
        if (ioHandler.exists(options.page)) {
            this.response.status(200)
        } else {
            this.response.status(404)
        }
        Json.mapper.writeValueAsString(mapOf("location" to ioHandler.getOutputPath()))
    }

    post("/render") {

        // We have to set this but the location is never used.
        request.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement("/tmp"))

        val file = this.request.raw().getPart("file")
        val body = this.request.raw().getPart("body")

        try {
            val backoff = backoffResponse()
            if (backoff != null) {
                this.response.status(429)
                Json.mapper.writeValueAsString(backoff)
            } else {
                val req = Json.mapper.readValue<RenderRequest>(body.inputStream)
                val doc = extract(req, file.inputStream)
                WorkQueue.execute(WorkQueueEntry(doc, req))

                this.response.status(201)
                Json.mapper.writeValueAsString(
                    mapOf(
                        "location" to doc.ioHandler.getOutputPath()
                    )
                )
            }
        } catch (e: Exception) {
            logger.warn("failed to process", e)
            this.response.status(500)
            Json.mapper.writeValueAsString(mapOf("status" to e.message))
        }
    }

    get("/monitor/health") {
        response.type("application/json")
        if (StorageManager.storageClient().bucketExists(Config.bucket.name)) {
            """{"status": "UP"}"""
        } else {
            response.status(400)
            """{"status": "DOWN"}"""
        }
    }
}

fun main(args: Array<String>) = try {
    Config.logSystemConfiguration()
    runServer(Config.officer.port)
} catch (e: Exception) {
    println(e.message)
    exitProcess(1)
}
