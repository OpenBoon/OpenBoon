package com.zorroa

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.Closeable
import java.io.InputStream
import java.lang.management.ManagementFactory
import java.util.Date
import java.util.UUID
import javax.servlet.MultipartConfigElement
import kotlin.system.exitProcess
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.kotlin.get
import spark.kotlin.post
import spark.kotlin.threadPool

const val ASPOSE_LICENSE_FILE = "Aspose.Total.Java.lic"

object ServerOptions {

    @Parameter(
        names = ["-c", "-config"],
        description = "Path to config file"
    )
    var configFile: String = System.getenv("SERVICE_CONFIG_FILE") ?: "./config/application.yml"
}

/**
 * All the options supported by Officer .
 */
class Options(val fileName: String) {

    @Parameter(names = ["-p", "-page"], description = "The page number to render. -1 for all pages")
    var page: Int = -1

    @Parameter(
        names = ["-o", "-output-dir"],
        description = "An output directory for the given request"
    )
    var outputDir: String = UUID.randomUUID().toString().replace("-", "")

    @Parameter(names = ["-v", "-verbose"], description = "Log extra information")
    var verbose: Boolean = false
}

/**
 * A utility object for json conversions. Filters null values.
 */
object Json {
    val mapper = jacksonObjectMapper()

    init {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}

/**
 * The minimal Document interface.
 */
abstract class Document(val options: Options) : Closeable {

    val ioHandler = IOHandler(options)

    fun renderImage() {
        renderImage(options.page)
    }

    abstract fun renderImage(page: Int)
    abstract fun renderAllImages(): Int

    fun renderMetadata() {
        renderMetadata(options.page)
    }

    abstract fun renderAllMetadata(): Int
    abstract fun renderMetadata(page: Int)

    fun render() {
        if (isRenderAll()) {
            logger.info("Rendering all images and metadata to {}", options.outputDir)
            renderAllImages()
            renderAllMetadata()
        } else {
            renderImage()
            renderMetadata()
        }
    }

    fun isRenderAll(): Boolean {
        return options.page < 0
    }

    fun logImageTime(page: Int, time: Long) {
        val mem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        logger.info(
            "proxy input='${options.fileName}' output='${ioHandler.getOutputUri()}' page='$page' in time='{}ms', freemem='{}m'",
            time,
            mem
        )
    }

    fun logMetadataTime(page: Int, time: Long) {
        val mem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        logger.info(
            "metadata input='${options.fileName}'  output='${ioHandler.getOutputUri()}' page='$page' in time='{}ms', freemem='{}m'",
            time,
            mem
        )
    }

    fun getMetadata(page: Int = 0): InputStream {
        return ioHandler.getMetadata(page)
    }

    fun getImage(page: Int = 0): InputStream {
        return ioHandler.getImage(page)
    }

    fun convertDate(date: Date?): String? {
        if (date == null) {
            return null
        }
        return try {
            date?.toInstant()?.toString()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Document::class.java)
        val whitespaceRegex = Regex("\\s+")
    }
}

/**
 * Extract the image and metadata to their resting place.
 */
fun extract(opts: Options, input: InputStream): Document {
    requireNotNull(opts.outputDir) { "An output directory must be provided" }

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

        // calls close automatically
        doc.use {
            it.render()
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

    val threads = (os.availableProcessors * 3).coerceAtLeast(8)
    logger.info("init web server: threads=$threads port=$port")

    spark.kotlin.port(port)
    threadPool(threads, threads, 600 * 1000)

    post("/exists") {
        val options = Json.mapper.readValue<Options>(this.request.body())
        val ioHandler = IOHandler(options)
        if (ioHandler.exists(options.page)) {
            this.response.status(200)
        } else {
            this.response.status(404)
        }
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
                val req = Json.mapper.readValue<Options>(body.inputStream)
                val doc = extract(req, file.inputStream)
                this.response.status(201)
                Json.mapper.writeValueAsString(mapOf("output" to doc.ioHandler.getOutputUri()))
            }
        } catch (e: Exception) {
            logger.warn("failed to process", e)
            this.response.status(500)
            Json.mapper.writeValueAsString(mapOf("status" to e.message))
        }
    }

    get("/status") {
        "OK\n"
    }
}

fun main(args: Array<String>) = try {

    val opts = ServerOptions
    val cmd = JCommander.newBuilder()
        .addObject(opts)
        .build()
    cmd.parse(*args)

    val heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024
    val maxHeapSize = Runtime.getRuntime().maxMemory() / 1024 / 1024

    println("Java heap size: ${heapSize}m")
    println("Java max heap size: ${maxHeapSize}m")
    runServer(Config.officer.port)
} catch (e: Exception) {
    println(e.message)
    exitProcess(1)
}
