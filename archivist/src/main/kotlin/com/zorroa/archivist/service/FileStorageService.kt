package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.SignUrlOption
import com.google.cloud.storage.StorageOptions
import com.google.common.base.Preconditions
import com.google.rpc.PreconditionFailure
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.util.StaticUtils
import com.zorroa.archivist.util.event
import com.zorroa.common.domain.Task
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * The FileStorageService is for determining the location of files associated with
 * assets, exports, etc.
 */
interface FileStorageService {

    /**
     * Use a FileStorageSpec to determine if a file already exists with the given spec.
     *
     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorage object detailing the location of the storage
     */
    fun get(spec: FileStorageSpec) : FileStorage

    /**
     * Use a FileStorage ID to get an existing FileStorage record
     *
     * @param[id] The unique id of the storage element
     * @return a FileStorage object detailing the location of the storage
     */
    fun get(id: String) : FileStorage

    /**
     * In order to read/write this image, callers need a signed URL.
     * @param[id] The unique id of the storage element
     * @param[method] The http method (put to write, get to read)
     * @param[duration] The duration the signed URL is active
     * @param[unit] The unit of time passed for duration
     * @return a FileStorage object detailing the location of the storage
     */
    fun getSignedUrl(id: String, method: HttpMethod, duration: Long=10, unit: TimeUnit=TimeUnit.MINUTES) : String

}

/**
 * A LayoutProvider is basically a path munger. All this logic could have gone into
 * the FileStorageService but it's nice to have to be separate so each class
 * munges with the same API.
 */
interface LayoutProvider {

    /**
     * Takes a FileStorageSpec and returns a URI for the file.
     *
     * @param spec the FileStorageSpec
     * @return the URI as a String
     */
    fun buildUri(spec: FileStorageSpec): String

    /**
     * Takes a unique ID and returns a URI.
     *
     * @param id the unique ID
     * @return the URI as a String
     */
    fun buildUri(id: String): String

    /**
     * Takes a FileStorageSpec and returns a URI.
     *
     * @param spec the FileStorageSpec
     * @return the unique ID
     */
    fun buildId(spec: FileStorageSpec): String
}

/**
 * The GcsFileStorageService handles the location and placement of files withing GCS.
 */
class GcsFileStorageService constructor(val bucket: String, credsFile: Path?=null) : FileStorageService {

    @Autowired
    lateinit var properties: ApplicationProperties

    @Autowired
    lateinit var fileServerProvider: FileServerProvider

    private val gcs: Storage

    val dlp  = GcsLayoutProvider(bucket)

    init {
        gcs = if (credsFile!= null && Files.exists(credsFile)) {
            StorageOptions.newBuilder().setCredentials(
                    GoogleCredentials.fromStream(FileInputStream(credsFile.toFile()))).build().service
        }
        else {
            StorageOptions.newBuilder().build().service
        }
    }

    override fun get(spec: FileStorageSpec): FileStorage {
        val uri = dlp.buildUri(spec)
        val id = dlp.buildId(spec)

        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to id,
                        "fileStorageUri" to uri))

        return FileStorage(id, uri,"gs", StaticUtils.tika.detect(uri), fileServerProvider)
    }

    override fun get(id: String): FileStorage {
        val storage =  FileStorage(
                unslashed(id),
                dlp.buildUri(id),
                "gs",
                StaticUtils.tika.detect(id),
                fileServerProvider)
        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to storage.id,
                        "fileStorageUri" to storage.uri))
        return storage
    }

    override fun getSignedUrl(id: String, method: HttpMethod, duration: Long, unit: TimeUnit) : String {
        val uri = URI(dlp.buildUri(id))
        val path = uri.path
        val contentType = StaticUtils.tika.detect(path)

        logger.event("sign FileStorage",
                mapOf("contentType" to contentType, "storageId" to uri, "bucket" to bucket, "path" to path))

        val info = BlobInfo.newBuilder(bucket, path).setContentType(contentType).build()
        return gcs.signUrl(info, duration, unit,
                SignUrlOption.withContentType(), SignUrlOption.httpMethod(method)).toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcsFileStorageService::class.java)
    }
}

/**
 * LocalFileStorageService handles the location of files in an on-prem single tenant install.
 */
class LocalFileStorageService constructor(
        val root: Path, ofs: ObjectFileSystem): FileStorageService {

    val dlp = LocalLayoutProvider(root, ofs)

    @Autowired
    lateinit var fileServerProvider: FileServerProvider

    init {
        logger.info("Initializing LocalFileStorageService at {}", root)
        if (!Files.exists(root)) {
            logger.info("LocalFileStorageService creating directory: {}", root)
            Files.createDirectories(root)
        }
        listOf("exports").forEach {
            val p = root.resolve(it)
            if (!Files.exists(p)) {
                logger.info("LocalFileStorageService creating directory: {}", p)
                Files.createDirectories(p)
            }
        }
    }

    override fun get(spec: FileStorageSpec) : FileStorage {
        return buildFileStorage(dlp.buildId(spec), dlp.buildUri(spec))
    }

    override fun get(id: String) : FileStorage {
        val url = dlp.buildUri(id)
        return buildFileStorage(id, url)
    }

    override fun getSignedUrl(id: String, method: HttpMethod, duration: Long, unit: TimeUnit) : String  {
        return dlp.buildUri(id)
    }

    private fun buildFileStorage(id: String, url: String) : FileStorage {
        val storage = FileStorage(
                unslashed(id),
                url,
                "file",
                StaticUtils.tika.detect(url),
                fileServerProvider)

        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to storage.id,
                        "fileStorageUri" to storage.uri))
        return storage
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileStorageService::class.java)
    }
}


class LocalLayoutProvider(val root: Path, private val ofs: ObjectFileSystem) : LayoutProvider {

    override fun buildUri(spec: FileStorageSpec): String {

        return if (spec.category == "export") {
            Preconditions.checkNotNull(spec.jobId, "Export locations must have a job Id")
            val path = root.resolve("exports")
                    .resolve(spec.jobId.toString())
                    .resolve("${spec.name}.${spec.type}")
            path.toFile().parentFile.mkdirs()
            path.toUri().toString()
        }
        else if (spec.category == "log") {
            Preconditions.checkNotNull(spec.jobId, "Log locations must have a job Id")
            Preconditions.checkNotNull(spec.taskId, "Log locations must have a task Id")
            val path = root.resolve("logs")
                    .resolve(spec.jobId.toString())
                    .resolve("${spec.name}.${spec.type}")
            path.toFile().parentFile.mkdirs()
            path.toUri().toString()
        }
        else {
            val name = spec.assetId ?: spec.name
            val ofile = ofs.prepare(spec.category, name, spec.type, spec.variants)
            ofile.file.toPath().toUri().toString()
        }
    }

    override fun buildUri(id: String): String {
        if (id.contains('/')) {
            throw IllegalArgumentException("Id '$id' cannot contain a slash")
        }

        val e = id.split("___")
        return when (e[0]) {
            "export" -> {
                root.resolve("exports").resolve(e[1]).resolve(e[2]).toUri().toString()
            }
            else -> {
                val sid = slashed(id)
                ofs.get(sid).path.toUri().toString()
            }
        }
    }

    override fun buildId(spec: FileStorageSpec): String {
        return when(spec.category) {
            "export" -> {
                Preconditions.checkNotNull(spec.jobId, "Export locations must have a job Id")
                "${spec.category}___${spec.jobId}___${spec.name}.${spec.type}"
            }
            else -> {
                val name = spec.assetId ?: spec.name
                unslashed(ofs.prepare(spec.category, name, spec.type, spec.variants).id)
            }
        }
    }
}


class GcsLayoutProvider(private val bucket: String) : LayoutProvider {

    override fun buildUri(id: String): String {
        val org = getOrgId()
        val slashed = slashed(id)
        return "gs://$bucket/orgs/$org/ofs/$slashed"
    }

    override fun buildUri(spec: FileStorageSpec) : String {
        val org = getOrgId()
        var variant = spec.variants?.joinToString("_", prefix="_") ?: ""
        if (variant == "_") {
            variant = ""
        }
        val id = getElementId(spec)
        return "gs://$bucket/orgs/$org/ofs/${spec.category}/$id/${spec.name}$variant.${spec.type}"
    }

    override fun buildId(spec: FileStorageSpec) : String {
        var variant = spec.variants?.joinToString("_", prefix="_") ?: ""
        if (variant == "_") {
            variant = ""
        }
        val id = getElementId(spec)
        return "${spec.category}___${id}___${spec.name}$variant.${spec.type}"
    }

    inline fun getElementId(spec: FileStorageSpec) : UUID {
        return if (spec.category == "export") {
            spec.jobId?.let {
                it
            }
            throw IllegalStateException("Cannot register export files without job id")
        }
        else {
            spec.assetId ?: StaticUtils.uuid3.generate(spec.name)
        }
    }

}

/**
 * Utility method for replacing slashes with triple ___ in proxy IDs
 */
private inline fun unslashed(id: String) : String {
    return id.replace("/", "___")
}

/**
 * Utility method for replacing ___ with triple / in proxy IDs
 */
private inline fun slashed(id: String) : String {
    return id.replace("___", "/")
}
