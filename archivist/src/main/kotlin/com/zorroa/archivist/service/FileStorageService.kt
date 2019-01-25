package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import com.google.cloud.storage.Storage.SignUrlOption
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.util.StaticUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * The allowed parent types for file storage.
 */
private val allowedParentTypes = setOf(
        "asset",
        "folder",
        "job",
        "pipeline",
        "user"
)

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

    /**
     * Write the given type array to location of th given ID.
     *
     * @param[id] The unique ID of the file.
     * @param[input] The array of bytes to write.
     */
    fun write(id: String, input: ByteArray)
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
            StorageOptions.newBuilder()
                    .setCredentials(
                    GoogleCredentials.fromStream(FileInputStream(credsFile.toFile()))).build().service
        }
        else {
            StorageOptions.newBuilder().build().service
        }
    }

    override fun get(spec: FileStorageSpec): FileStorage {
        val uri = dlp.buildUri(spec)
        val id = dlp.buildId(spec)

        logger.event(LogObject.STORAGE, LogAction.GET,
                mapOf("fileStorageId" to id,
                        "fileStorageUri" to uri))

        return FileStorage(id, URI(uri),"gs", StaticUtils.tika.detect(uri), fileServerProvider)
    }

    override fun get(id: String): FileStorage {
        val storage =  FileStorage(
                unslashed(id),
                URI(dlp.buildUri(id)),
                "gs",
                StaticUtils.tika.detect(id),
                fileServerProvider)
        logger.event(LogObject.STORAGE, LogAction.GET,
                mapOf("fileStorageId" to storage.id,
                        "fileStorageUri" to storage.uri))
        return storage
    }

    override fun getSignedUrl(id: String, method: HttpMethod, duration: Long, unit: TimeUnit) : String {
        val uri = URI(dlp.buildUri(id))
        val path = uri.path
        val contentType = StaticUtils.tika.detect(path)

        logger.event(LogObject.STORAGE, LogAction.AUTHORIZE,
                mapOf("method" to method.toString(), "contentType" to contentType,
                        "storageId" to uri, "bucket" to bucket, "path" to path))

        val info = BlobInfo.newBuilder(bucket, path).setContentType(contentType).build()
        val opts = if (method == HttpMethod.PUT) {
            arrayOf(SignUrlOption.withContentType(), SignUrlOption.httpMethod(method))
        }
        else {
            arrayOf(SignUrlOption.httpMethod(method))
        }

        return gcs.signUrl(info, duration, unit, *opts).toString()
    }

    override fun write(id: String, input: ByteArray) {
        val uri = URI(dlp.buildUri(id))
        val blobId = BlobId.of(bucket, uri.path.substring(1))
        logger.event(LogObject.STORAGE, LogAction.WRITE, mapOf("uri" to uri))
        gcs.create(BlobInfo.newBuilder(blobId).build(), input)
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
    }

    override fun get(spec: FileStorageSpec) : FileStorage {
        return buildFileStorage(dlp.buildId(spec), dlp.buildUri(spec))
    }

    override fun get(id: String) : FileStorage {
        val url = dlp.buildUri(id)
        return buildFileStorage(id, url)
    }

    override fun getSignedUrl(id: String, method: HttpMethod, duration: Long, unit: TimeUnit) : String  {
        val url = dlp.buildUri(id)
        if (method == HttpMethod.PUT) {
            val parent = Paths.get(URI(url)).toFile().parentFile
            parent.mkdirs()
        }
        return url
    }

    override fun write(id: String, input: ByteArray) {
        val uri = Paths.get(URI(dlp.buildUri(id)))
        val parent = uri.parent
        Files.createDirectories(parent)
        logger.event(LogObject.STORAGE, LogAction.WRITE, mapOf("uri" to uri))
        Files.write(uri, input)
    }

    private fun buildFileStorage(id: String, url: String) : FileStorage {
        return FileStorage(
                unslashed(id),
                URI(url),
                "file",
                StaticUtils.tika.detect(url),
                fileServerProvider)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileStorageService::class.java)
    }
}


class LocalLayoutProvider(val root: Path, private val ofs: ObjectFileSystem) : LayoutProvider {

    override fun buildUri(spec: FileStorageSpec): String {
        val id = buildId(spec)
        return buildUri(id)
    }

    override fun buildUri(id: String): String {
        // 0.39 backwards compatible.
        if (id.startsWith("proxy/")) {
            return ofs.get(id).path.toUri().toString()
        }

        val e = id.split("___")
        var path = getOrgRoot().resolve(e[0]).resolve(expandId(e[1]))
        for (item in e.subList(2, e.size)) {
            path = path.resolve(item)
        }
        return path.toUri().toString()
    }

    override fun buildId(spec: FileStorageSpec): String {
        val name = unslash_name(spec.name)

        val parentType = spec.parentType.toLowerCase()
        if (parentType !in allowedParentTypes) {
            throw IllegalArgumentException("Invalid parent Type: $parentType")
        }
        return "${parentType}___${spec.parentId.toLowerCase()}___$name"
    }

    private fun getOrgRoot() : Path  {
        return root.resolve("orgs").resolve(getOrgId().toString())
    }

    private fun expandId(id: String) : String {
        val sb = StringBuilder(16)
        for (i in 0..3) {
            sb.append("${id[i]}/")
        }
        sb.append(id)
        return sb.toString()
    }
}


class GcsLayoutProvider(private val bucket: String) : LayoutProvider {

    override fun buildUri(id: String): String {
        if (id.startsWith("proxy___")) {
            /**
             * Handle the old style GCS slugs
             *
             * proxy___098c296c-33dd-594a-827c-26118ff62882___098c296c-33dd-594a-827c-26118ff62882_256x144.jpg
             */
            return "${getOrgRoot()}/ofs/${slashed(id)}"
        }

        if (id.contains('/')) {
            throw IllegalArgumentException("Id '$id' cannot contain a slash")
        }
        val slashed = slashed(id)
        return "${getOrgRoot()}/$slashed"
    }

    override fun buildUri(spec: FileStorageSpec) : String {
        return "${getOrgRoot()}/${spec.parentType}/${spec.parentId}/${spec.name}"
    }

    override fun buildId(spec: FileStorageSpec) : String {
        val name = unslash_name(spec.name)
        val parentType = spec.parentType.toLowerCase()
        if (parentType !in allowedParentTypes) {
            throw IllegalArgumentException("Invalid parent Type: $parentType")
        }
        return "${spec.parentType}___${spec.parentId}___$name"
    }

    private fun getOrgRoot() : String  {
        val org = getOrgId()
        return "gs://$bucket/orgs/$org"
    }

}

/**
 * Utility function for cleaning up file slugs.
 */
private inline fun unslash_name(name: String) : String {
    return name
        .replace(Regex("[/]+"), "___")
        .replace(Regex("\\.{2,}"), ".")
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
