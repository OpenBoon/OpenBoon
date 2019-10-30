package com.zorroa

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.google.common.net.UrlEscapers
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.kotlin.get
import spark.kotlin.post
import spark.kotlin.threadPool
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.management.ManagementFactory
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

const val ASPOSE_LICENSE_FILE = "Aspose.Total.Java.lic"

object ServerOptions {

    @Parameter(
        names = ["-s", "-storage-path"],
        description = "The root storage path for batch output"
    )
    var storagePath: String = System.getenv("ZORROA_STORAGE_PATH") ?: "/tmp/zextract"

    @Parameter(names = ["-v", "-verbose"], description = "Log extra information")
    var verbose: Boolean = false

    @Parameter(names = ["-p", "-port"], description = "Listen TCP port")
    var port: Int = 7017

    @Parameter(names = ["-l", "-load-factor"], description = "Load factor multiplier before backoff")
    var loadMultipler: Int = 2

    @Parameter(
        names = ["-c", "-cleanup-days"],
        description = "Number of days before cleaning up old"
    )
    var cleanupDays: Int = 2
}

/**
 * All the options supported by zextract.
 */
class Options(path: String? = null) {

    constructor() : this(null)

    @Parameter(description = "Path to the office inputFile")
    var inputFile: String? = path

    @Parameter(names = ["-p", "-page"], description = "The page number to render. -1 for all pages")
    var page: Int = -1

    @Parameter(names = ["-c", "-content"], description = "Extract page content to metadata file.")
    var content: Boolean = false

    @Parameter(
        names = ["-o", "-output-dir"],
        description = "An output directory for the given request"
    )
    var outputDir: String = UUID.randomUUID().toString()

    @Parameter(names = ["-d", "-dpi"], description = "Resolution of output image, defaults to 75 DPI.")
    var dpi: Int = 75

    @Parameter(names = ["-v", "-verbose"], description = "Log extra information")
    var verbose: Boolean = false
}

/**
 * A utility object for json conversions. Filters null values.
 */
object Json {
    val mapper = ObjectMapper()

    init {
        mapper.registerModule(KotlinModule())
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
}

/**
 * A singleton object for getting files from GCS.
 */
object GoogleCloudStorage {

    private val storage: Storage

    init {
        val credentials = System.getenv("ZORROA_SERVICE_CREDENTIALS_PATH")
        storage = if (credentials != null) {
            val credsPath = Paths.get(credentials).toFile()
            StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream(credsPath))
            ).build().service
        } else {
            StorageOptions.newBuilder().build().service
        }
    }

    fun getBlob(uri: URI): Blob? {
        var (bucket, path) = splitGcpUrl(uri)
        val blobId = BlobId.of(bucket, path)
        return storage.get(blobId)
    }

    private fun splitGcpUrl(url: URI): Array<String> {
        return arrayOf(
            url.authority,
            url.path.removePrefix("/")
        )
    }
}

/**
 * Manages render inputs and outputs.
 */
class IOHandler(val options: Options) {

    /**
     * Keep track of any temp files we create so they
     * can be removed later.
     */
    private val tempFiles = mutableSetOf<Path>()

    val outputRoot = createOutputRoot()

    fun getInputPath(): String {
        requireNotNull(options.inputFile) { "Missing inputFile" }
        val inputFile = options.inputFile as String

        return if (inputFile.startsWith("gs://")) {
            val path = Files.createTempFile("gs_zorroa", "." + FilenameUtils.getExtension(inputFile))
            tempFiles.add(path)
            val blob = GoogleCloudStorage.getBlob(URI(UrlEscapers.urlFragmentEscaper().escape(options.inputFile)))
            logger.info("Downloading $inputFile to $path")
            requireNotNull(blob) { "Invalid GCS url: ${options.inputFile}" }
            blob.downloadTo(path)
            path.toString()
        } else {
            inputFile
        }
    }

    fun getImagePath(page: Int): Path {
        return outputRoot.resolve("proxy.$page.jpg")
    }

    fun getMetadataPath(page: Int): Path {
        return outputRoot.resolve("metadata.$page.json")
    }

    fun createOutputRoot(): Path {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val mon = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = (cal.get(Calendar.DAY_OF_MONTH)).toString().padStart(2, '0')

        val root = Paths.get(ServerOptions.storagePath)
            .resolve("${year}${mon}$day")
            .resolve(options.outputDir.removePrefix("/").replace("..", "_"))

        logger.info("creating output directory: $root")
        Files.createDirectories(
            root,
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x"))
        )
        return root
    }

    fun removeTempFiles() {
        tempFiles.forEach {
            try {
                logger.info("removing temp $it")
                Files.delete(it)
            } catch (e: IOException) {
                logger.warn("Failed to delete temp file: $it", e)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IOHandler::class.java)
    }
}

fun fileExistsAndNonZeroSize(path: Path): Boolean {
    return if (Files.exists(path)) {
        Files.size(path) > 0L
    } else {
        false
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
    abstract fun renderAllImages()

    fun renderMetadata() {
        renderMetadata(options.page)
    }

    abstract fun renderAllMetadata()
    abstract fun renderMetadata(page: Int)

    fun render() {
        if (isRenderAll()) {
            renderAllImages()
            renderAllMetadata()
        } else {
            val imageFile = getImageFile(options.page).toPath()
            if (!fileExistsAndNonZeroSize(imageFile)) {
                renderImage()
            } else {
                logger.info("{} already exists", imageFile)
            }

            val metadataFile = getMetadataFile(options.page).toPath()
            if (!fileExistsAndNonZeroSize(metadataFile)) {
                renderMetadata()
            } else {
                logger.info("{} already exists", metadataFile)
            }
        }

        ioHandler.removeTempFiles()
    }

    fun isRenderAll(): Boolean {
        return options.page < 1
    }

    fun logImageTime(page: Int, time: Long) {
        val mem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        logger.info("proxy input='${options.inputFile}' page='$page' in time='{}ms', freemem='{}m'", time, mem)
    }

    fun logMetadataTime(page: Int, time: Long) {
        val mem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        logger.info("metadata input='${options.inputFile}' page='$page' in time='{}ms', freemem='{}m'", time, mem)
    }

    fun getOutputRoot(): Path {
        return ioHandler.outputRoot
    }

    fun getImagePath(page: Int? = null): Path {
        return ioHandler.getImagePath(page ?: options.page)
    }

    fun getImageFile(page: Int? = null): File {
        return getImagePath(page).toFile()
    }

    fun getMetadataPath(page: Int? = null): Path {
        return ioHandler.getMetadataPath(page ?: options.page)
    }

    fun getMetadataFile(page: Int? = null): File {
        return getMetadataPath(page).toFile()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(Document::class.java)
        val whitespaceRegex = Regex("\\s+")
    }
}

object StorageManager {

    val logger: Logger = LoggerFactory.getLogger(StorageManager::class.java)
    val dateRegex = Regex("^\\d{4}\\d{2}\\d{2}$")
    val storageRoot = Paths.get(ServerOptions.storagePath)

    init {
        createStorageRoot()
        setupStorageCleanupTimer()
    }

    private fun setupStorageCleanupTimer() {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                cleanup()
            }
        }, TimeUnit.MINUTES.toMillis(1), TimeUnit.HOURS.toMillis(1))
    }

    private fun createStorageRoot() {
        IOHandler.logger.info("creating storage root directory: $storageRoot")
        Files.createDirectories(storageRoot)
    }

    fun cleanup(): Int {
        try {
            logger.info("Cleaning up old data, cutoff is ${ServerOptions.cleanupDays} days.")
            val dateFormatter = SimpleDateFormat("yyyyMMdd")
            val now = Date()
            var count = 0

            Files.list(storageRoot).forEach {
                val name = it.fileName.toString()
                if (dateRegex.matches(name)) {
                    val date = dateFormatter.parse(name)
                    val diff = now.time - date.time
                    val diffInDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
                    if (diffInDays >= ServerOptions.cleanupDays) {
                        logger.info("cleaning up directory: $it, $diffInDays days old")
                        FileUtils.deleteDirectory(it.toFile())
                        count += 1
                    }
                }
            }
            if (count > 0) {
                logger.info("Cleaned up $count directories")
            }
            return count
        } catch (e: Exception) {
            logger.warn("Failed to clean up output storage")
        }

        return 0
    }
}

/**
 * Extract the image and metadata to their resting place.
 */
fun extract(opts: Options): Document {
    requireNotNull(opts.outputDir) { "An output directory must be provided" }

    val fileExt = opts.inputFile
        ?.substringAfterLast('.', "")
        ?.toLowerCase()
    val doc = when (fileExt) {
        "pdf" -> PdfDocument(opts)
        "ppt", "pptx", "odp" -> SlidesDocument(opts)
        "doc", "docx" -> WordDocument(opts)
        "xls", "xlsx" -> CellsDocument(opts)
        "vsd", "vsdx" -> VisioDocument(opts)
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

fun backoffResponse(): Map<String, Any>? {
    val os = ManagementFactory.getOperatingSystemMXBean()
    val maxLoad = os.availableProcessors * ServerOptions.loadMultipler
    return if (os.systemLoadAverage > maxLoad) {
        mapOf(
            "load" to os.systemLoadAverage,
            "max" to os.availableProcessors * ServerOptions.loadMultipler
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

    // Init the storage manager
    StorageManager

    val threads = (os.availableProcessors * 3).coerceAtLeast(8)
    logger.info("init web server: threads=$threads port=$port")

    spark.kotlin.port(port)
    threadPool(threads, threads, 600 * 1000)
    post("/ocr") {
        val body = this.request.body()
        try {
            val backoff = backoffResponse()
            if (backoff != null) {
                this.response.status(429)
                Json.mapper.writeValueAsString(backoff)
            } else {
                val req = Json.mapper.readValue<Options>(body)
                val doc = OcrDocument(req)
                doc.use {
                    it.render()
                }
                this.response.status(201)
                Json.mapper.writeValueAsString(mapOf("output" to doc.getOutputRoot().toString()))
            }
        } catch (e: Exception) {
            logger.warn("failed to process $body", e)
            this.response.status(500)
            Json.mapper.writeValueAsString(mapOf("status" to e.message))
        }
    }

    post("/extract") {

        val body = this.request.body()
        try {
            val backoff = backoffResponse()
            if (backoff != null) {
                this.response.status(429)
                Json.mapper.writeValueAsString(backoff)
            } else {
                val req = Json.mapper.readValue<Options>(body)
                val doc = extract(req)
                this.response.status(201)
                Json.mapper.writeValueAsString(mapOf("output" to doc.getOutputRoot().toString()))
            }
        } catch (e: Exception) {
            logger.warn("failed to process $body", e)
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

    if (opts.storagePath == "/tmp/zextract") {
        println("Storage path set to /tmp/zextract, this is not good for production cases")
    }

    val heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024
    val maxHeapSize = Runtime.getRuntime().maxMemory() / 1024 / 1024

    println("Java heap size: ${heapSize}m")
    println("Java max heap size: ${maxHeapSize}m")

    runServer(opts.port)
} catch (e: Exception) {
    println(e.message)
    exitProcess(1)
}
