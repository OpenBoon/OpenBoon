package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.SignUrlOption
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.filesystem.OfsFile
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.util.StaticUtils
import com.zorroa.archivist.util.event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit


/**
 * The FileStorageService is for determining the location of files assocaiated with
 * assets, exports, etc.
 *
 */
interface FileStorageService {

    /**
     * Allocates new file storage.
     *
     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorage object detailing the location of the storage
     */
    fun create(spec: FileStorageSpec) : FileStorage

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
     * @return a FileStorage object detailing the location of the storage
     */
    fun getSignedUrl(id: String, method: HttpMethod) : String

}

class GcsFileStorageService constructor(val bucket: String, credsFile: Path?=null) : FileStorageService {

    @Autowired
    lateinit var properties: ApplicationProperties

    private val gcs: Storage

    val dlp : DirectoryLayoutProvider

    init {

        gcs = if (credsFile!= null && Files.exists(credsFile)) {
            StorageOptions.newBuilder().setCredentials(
                    GoogleCredentials.fromStream(FileInputStream(credsFile.toFile()))).build().service
        }
        else {
            StorageOptions.newBuilder().build().service
        }

        dlp = GcsDirectoryLayoutProvider(bucket)
    }

    override fun create(spec: FileStorageSpec): FileStorage {
        val uri = dlp.buildUri(spec)
        val id = dlp.buildId(spec)
        val storage =  FileStorage(id, uri,"gs", StaticUtils.tika.detect(uri))

        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to storage.id,
                        "fileStorageUri" to storage.uri))
        return storage
    }

    override fun get(spec: FileStorageSpec): FileStorage {
        val uri = dlp.buildUri(spec)
        val id = dlp.buildId(spec)

        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to id,
                        "fileStorageUri" to uri))

        return FileStorage(id, uri,"gs", StaticUtils.tika.detect(uri))
    }

    override fun get(id: String): FileStorage {
        val storage =  FileStorage(unslashed(id), dlp.buildUri(id), "gs", StaticUtils.tika.detect(id))
        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to storage.id,
                        "fileStorageUri" to storage.uri))
        return storage
    }

    override fun getSignedUrl(id: String, method: HttpMethod) : String {
        val uri = URI(dlp.buildUri(id))
        val path = uri.path
        val contentType = StaticUtils.tika.detect(path)

        logger.event("sign StorageFile",
                mapOf("contentType" to contentType, "storageId" to uri, "bucket" to bucket, "path" to path))

        val info = BlobInfo.newBuilder(bucket, path).setContentType(contentType).build()
        return gcs.signUrl(info, 10, TimeUnit.MINUTES,
                SignUrlOption.withContentType(), SignUrlOption.httpMethod(method)).toString()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcsFileStorageService::class.java)
    }
}

/**
 * LocalFileStorageService handles where files are stored in an on-prem single
 * tenant install.
 */
class LocalFileStorageService @Autowired constructor(
        val root: Path, private val ofs: ObjectFileSystem): FileStorageService {

    init {
        logger.info("Initializing LocalFileStorageService at {}", root)
        if (!Files.exists(root)) {
            logger.info("LocalFileStorageService creating directory: {}", root)
            Files.createDirectories(root)
        }
        listOf("exports", "models").forEach {
            val p = root.resolve(it)
            if (!Files.exists(p)) {
                logger.info("LocalFileStorageService creating directory: {}", p)
                Files.createDirectories(p)
            }
        }
    }

    override fun create(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.prepare(spec.category, spec.name, spec.type, spec.variants)
        return buildFileStorage(ofile)
    }

    override fun get(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.get(spec.category, spec.name, spec.type, spec.variants)
        return buildFileStorage(ofile)
    }

    override fun get(id: String) : FileStorage {
        val ofile = ofs.get(slashed(id))
        return buildFileStorage(ofile)
    }

    override fun getSignedUrl(id: String, method: HttpMethod) : String  {
        val ofile = ofs.get(slashed(id))
        return ofile.file.toURI().toString()
    }

    private fun buildFileStorage(ofile: OfsFile) : FileStorage {
        val storage = FileStorage(
                unslashed(ofile.id),
                ofile.file.toURI().toString(),
                "file",
                StaticUtils.tika.detect(ofile.file.toString()))

        logger.event("getLocation FileStorage",
                mapOf("fileStorageId" to storage.id,
                        "fileStorageUri" to storage.uri))
        return storage
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileStorageService::class.java)
    }
}

interface DirectoryLayoutProvider {
    fun buildUri(st: FileStorageSpec): String
    fun buildUri(id: String): String
    fun buildId(spec: FileStorageSpec): String
}

class GcsDirectoryLayoutProvider(bucket: String) : DirectoryLayoutProvider {

    private val dlpDefault = DefaultGcsDirectoryLayoutProvider(bucket)

    override fun buildUri(st: FileStorageSpec): String {
        return dlpDefault.buildUri(st)
    }

    override fun buildUri(id: String): String {
        return dlpDefault.buildUri(id)
    }

    override fun buildId(spec: FileStorageSpec): String {
        return dlpDefault.buildId(spec)
    }
}

class DefaultGcsDirectoryLayoutProvider(private val bucket: String) : DirectoryLayoutProvider {

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
        val assetId = spec.assetId ?: StaticUtils.uuid3.generate(spec.name)
        return "gs://$bucket/orgs/$org/ofs/${spec.category}/$assetId/${spec.name}$variant.${spec.type}"
    }

    override fun buildId(spec: FileStorageSpec) : String {
        var variant = spec.variants?.joinToString("_", prefix="_") ?: ""
        if (variant == "_") {
            variant = ""
        }
        val assetId = spec.assetId ?: StaticUtils.uuid3.generate(spec.name)
        return "${spec.category}___${assetId}___${spec.name}$variant.${spec.type}"
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


