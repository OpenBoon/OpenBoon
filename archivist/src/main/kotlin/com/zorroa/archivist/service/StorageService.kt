package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.config.ApplicationProperties
import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.nio.channels.Channels
import javax.annotation.PostConstruct

/**
 * StorageService is a WIP attempt to handle both GCP, AWS, and possibly OFS.
 * For now there is an AWS version, and stubs for a GCP version.
 *
 * It's not clear all of these methods are going to be used or if any of this
 * code moves forward after MVP.
 */

interface StorageService {

    /**
     * Return an ObjectFile with an open stream to the file, its name, size,
     * and mimeType.
     *
     * @param url: A full URL to a GCS resource
     * @param mimeType: An optional mimeType for the object stream
     */
    fun getObjectFile(url: URL, mimeType:String?=null): ObjectFile

    /**
     * Return true of the Object exists.
     */
    fun objectExists(url: URL): Boolean
}

data class ObjectFile (
        val stream: InputStream,
        val name: String,
        val size: Long,
        val type: String?)


open class ZorroaStorageException(e: Exception) : RuntimeException(e)
class StorageWriteException (e: Exception) : ZorroaStorageException(e)
class StorageReadException (e: Exception) : ZorroaStorageException(e)


class GcpStorageService @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {

    lateinit var storage: Storage

    @PostConstruct
    fun setup() {
        val credentials = properties.getString("archivist.storage.gcp.credentials")
        storage = StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream(credentials))).build().service
    }

    override fun objectExists(url: URL): Boolean {
        var (bucket, path) =  splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        val storage = storage.get(blobId) ?: return false
        return storage.exists()
    }

    override fun getObjectFile(url: URL, mimeType:String?): ObjectFile {
        var (bucket, path) =  splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        val storage =  storage.get(blobId)

        return ObjectFile(
                Channels.newInputStream(storage.reader()),
                storage.name,
                storage.size,
                mimeType ?: storage.contentType)
    }

    private fun splitGcpUrl(url: URL) : Array<String> {
        val path = url.path
        return arrayOf (
            path.substring(path.indexOf('/') + 1, path.indexOf('/', 1)),
            path.substring(path.indexOf('/', 1) + 1)
        )
    }
}


class MinioStorageImpl @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {
    override fun objectExists(url: URL): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getObjectFile(url: URL, mimeType: String?): ObjectFile {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val client: MinioClient = MinioClient(
            properties.getString("archivist.storage.minio.endpoint"),
            properties.getString("archivist.storage.minio.accessKey"),
            properties.getString("archivist.storage.minio.secretKey"))
}
