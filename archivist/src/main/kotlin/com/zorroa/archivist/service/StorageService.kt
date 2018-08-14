package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.common.domain.Document
import com.zorroa.common.filesystem.ObjectFileSystem
import com.zorroa.common.schema.Proxy
import io.minio.MinioClient
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse


private val tika = Tika()

private val defaultContentType = "application/octet-steam"

/**
 * On object that can be stored somewhere in the vast reaches of the interweb.
 */
data class ObjectStat(val size: Long, val contentType: String, val exists: Boolean)

class ObjectFile (
        val storageService: StorageService,
        val uri: URI) {

    fun exists() : Boolean {
        return storageService.objectExists(uri)
    }

    fun isLocal() : Boolean {
        return storageService.storedLocally
    }

    fun getSignedUrl() : URL {
        return storageService.getSignedUrl(uri)
    }

    fun getReponseEntity() : ResponseEntity<InputStreamResource> {
        return storageService.getReponseEntity(uri)
    }

    fun copyTo(response: HttpServletResponse) {
        return storageService.copyTo(uri, response)
    }

    fun copyTo(stream: OutputStream) {
        return storageService.copyTo(uri, stream)
    }

    fun getLocalFile() : Path? {
        return storageService.getLocalPath(uri)
    }

    fun getStat(): ObjectStat {
        return storageService.getStat(uri)
    }

    /**
     * Currently unsupported
     */
    fun delete() : Boolean {
        return false;
    }
}

/**
 * StorageService is a WIP attempt to handle both GCP, AWS, and possibly OFS.
 * For now there is an AWS version, and stubs for a GCP version.
 *
 * It's not clear all of these methods are going to be used or if any of this
 * code moves forward after MVP.
 */
interface StorageRouter {

    fun getStorageUri(proxy: Proxy) : URI {
        return if (proxy.stream != null) {
            URI(proxy.stream)
        }
        else {
            URI("ofs://${proxy.id}")
        }
    }

    fun getStorageUri(doc: Document) : URI {
        val stream = doc.getAttr("source.stream", String::class.java)
        return if (stream != null) {
            URI(stream)
        }
        else {
            val path = doc.getAttr("source.path", String::class.java)
            URI("file://$path")
        }
    }

    fun getObjectFile(uri: URI): ObjectFile
    fun getObjectFile(proxy: Proxy): ObjectFile
    fun getObjectFile(doc: Document): ObjectFile
}

@Component
class StorageRouterImpl @Autowired constructor (
        val properties: ApplicationProperties,
        ofs: ObjectFileSystem) : StorageRouter {

    private val services : Map<String, StorageService>
    private val types: Set<String> = properties.getList("archivist.storage.types").toSet()

    init {

        services = mutableMapOf()
        if (types.contains("gcp")) {
            logger.info("Initializing storage: GCP")
            services["gcp"] = GcpStorageService(properties)
        }
        if (types.contains("ofs")) {
            logger.info("Initializing storage: OFS")
            services["ofs"] = OfsStorageService(properties, ofs)
        }
        if (types.contains("local")) {
            logger.info("Initializing storage: LOCAL")
            services["local"] = LocalStorageService(properties)
        }
        if (types.contains("remote")) {
            logger.info("Initializing storage: REMOTE")
            services["remote"] = RemoteStorageService(properties)
        }
    }

    fun getStorageService(uri: URI) : StorageService {
        val type = when(uri.scheme) {
            "http", "https"-> {
                if (uri.host.contains("google")) {
                    "gcp"
                }
                else {
                    "remote"
                }
            }
            "gs" -> "gcp"
            "file" -> "local"
            null->"local"
            else -> uri.scheme
        }

        if (!types.contains(type)) {
            throw ZorroaStorageException(
                    "Unable to find storage service for: $type")
        }

        return services[type] ?: throw ZorroaStorageException(
                "Unable to find storage service for: $type")
    }

    override fun getObjectFile(uri: URI): ObjectFile {
        val service = getStorageService(uri)
        return ObjectFile(service, uri)
    }

    override fun getObjectFile(doc: Document): ObjectFile {
        val uri = getStorageUri(doc)
        return getObjectFile(uri)
    }

    override fun getObjectFile(proxy: Proxy): ObjectFile {
        val uri = getStorageUri(proxy)
        return getObjectFile(uri)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StorageRouterImpl::class.java)
    }
}

interface StorageService {

    val storedLocally : Boolean

    fun getReponseEntity(url: URI) : ResponseEntity<InputStreamResource>

    fun copyTo(url: URI, response: HttpServletResponse)

    fun copyTo(url: URI, output: OutputStream)

    fun objectExists(url: URI): Boolean

    fun getSignedUrl(url: URI): URL

    fun getLocalPath(url: URI) : Path?

    fun getStat(url: URI) : ObjectStat
}

open class ZorroaStorageException(override var message:String?) : RuntimeException(message) {
    constructor(e: Exception) : this(e.message) {
        this.initCause(e)
    }
}

class StorageReadException (override var message:String?) : ZorroaStorageException(message)

/**
 * A storage service that handles files stored on a remote http(s) server.
 * Not fully implemented
 */
class RemoteStorageService @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyTo(url: URI, output: OutputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun objectExists(url: URI): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSignedUrl(url: URI): URL {
       return url.toURL()
    }

    override fun getLocalPath(url: URI): Path? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getStat(url: URI): ObjectStat {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val storedLocally: Boolean
        get() = false



}
class LocalStorageService @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {

    override val storedLocally: Boolean
        get() = true

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val path = Paths.get(url)
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tika.detect(path)))
                .contentLength(Files.size(path))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(FileInputStream(path.toFile())))
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        val path = Paths.get(url)
        response.setContentLengthLong(Files.size(path))
        response.contentType = tika.detect(path)
        FileInputStream(path.toFile()).copyTo(response.outputStream)
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val path = Paths.get(url)
        FileInputStream(path.toFile()).copyTo(output)
    }

    override fun objectExists(url: URI): Boolean {
        return File(url).exists()
    }

    override fun getSignedUrl(url: URI): URL {
        return url.toURL()
    }

    override fun getLocalPath(url: URI): Path? {
      return Paths.get(url)
    }

    override fun getStat(url: URI): ObjectStat {
       return try {
           val path = getLocalPath(url)
           ObjectStat(Files.size(path), tika.detect(path), objectExists(url))
       } catch (e: Exception) {
           ObjectStat(0, defaultContentType, false)
       }
    }
}


class OfsStorageService @Autowired constructor (
        val properties: ApplicationProperties,
        val ofs: ObjectFileSystem) : StorageService {

    override val storedLocally: Boolean
        get() = true

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val ofsFile = ofs.get(url.toString())
        val path = ofsFile.file.toPath()
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tika.detect(path)))
                .contentLength(Files.size(path))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(FileInputStream(path.toFile())))
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        val ofsFile = ofs.get(url.toString())
        val path = ofsFile.file.toPath()
        response.setContentLengthLong(Files.size(path))
        response.contentType = tika.detect(path)
        FileInputStream(ofsFile.file).copyTo(response.outputStream)
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val ofsFile = ofs.get(url.toString())
        FileInputStream(ofsFile.file).copyTo(output)
    }

    override fun objectExists(url: URI): Boolean {
        val ofsFile = ofs.get(url.toString())
        return ofsFile.exists()
    }

    override fun getSignedUrl(url: URI): URL {
        return url.toURL()
    }

    override fun getLocalPath(url: URI): Path? {
        val ofsFile = ofs.get(url.toString())
        return ofsFile.file.toPath()
    }

    override fun getStat(url: URI): ObjectStat {
        val path = getLocalPath(url)
        return ObjectStat(Files.size(path), tika.detect(path), objectExists(url))
    }
}


class GcpStorageService constructor (
        val properties: ApplicationProperties) : StorageService {

    private val storage: Storage
    init {
        val configPath = properties.getPath("archivist.config.path")
        storage = StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream(configPath.resolve("data-credentials.json").toFile()))).build().service
    }

    override val storedLocally: Boolean
        get() = false

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val blob =  getBlob(url)
        if (blob != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(blob.contentType))
                    .contentLength(blob.size)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(Channels.newInputStream(blob.reader())))
        } else {
            throw StorageReadException("$url not found")
        }
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        val blob =  getBlob(url)
        if (blob != null) {
            response.setContentLengthLong(blob.size)
            response.contentType = blob.contentType
            Channels.newInputStream(blob.reader()).copyTo(response.outputStream)
        } else {
            throw StorageReadException("$url not found")
        }
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val blob =  getBlob(url)
        if (blob != null) {
            Channels.newInputStream(blob.reader()).copyTo(output)
        } else {
            throw StorageReadException("$url not found")
        }
    }

    override fun getSignedUrl(url: URI): URL {
        val blob = getBlob(url)
        if (blob != null) {
            return blob.signUrl(60, TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET))
        }
        else {
            throw StorageReadException("$url not found")
        }
    }

    override fun objectExists(url: URI): Boolean {
        var (bucket, path) =  splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        val storage = storage.get(blobId) ?: return false
        return storage.exists()
    }

    private fun getBlob(url: URI) : Blob? {
        var (bucket, path) =  splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        return storage.get(blobId)
    }

    private fun splitGcpUrl(url: URI) : Array<String> {
        val path = url.path
        return arrayOf (
            path.substring(path.indexOf('/') + 1, path.indexOf('/', 1)),
            path.substring(path.indexOf('/', 1) + 1)
        )
    }

    override fun getLocalPath(url: URI): Path? {
       return null
    }

    override fun getStat(url: URI): ObjectStat {
        val blob = getBlob(url)
        return if (blob != null) {
            ObjectStat(blob.size, blob.contentType, objectExists(url))
        }
        else {
            ObjectStat(0, defaultContentType, false)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpStorageService::class.java)
    }
}


class MinioStorageImpl @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {

    override val storedLocally: Boolean
        get() = false


    override fun getStat(url: URI): ObjectStat {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun copyTo(url: URI, output: OutputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSignedUrl(url: URI): URL {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun objectExists(url: URI): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLocalPath(url: URI): Path? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private val client: MinioClient = MinioClient(
            properties.getString("archivist.storage.minio.endpoint"),
            properties.getString("archivist.storage.minio.accessKey"),
            properties.getString("archivist.storage.minio.secretKey"))

}
