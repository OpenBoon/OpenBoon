package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.common.domain.Asset
import com.zorroa.common.util.Json
import io.minio.MinioClient
import io.minio.errors.MinioException
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
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

fun getBucketName(asset: Asset) : String {
    return "${asset.organizationId}"
}

fun getFileName(asset: Asset, name:String?=null) : String {
    val prefix = asset.id.toString().subSequence(0, 4)
    return if (name == null) {
        "$prefix/${asset.id}/source"
    } else {
        "$prefix/${asset.id}/$name"
    }
}

/**
 * Most methods in here are deprecated or not needed by the Archivist.
 */
interface StorageService {

    fun createBucket(asset: Asset)
    fun removeBucket(asset: Asset)
    fun bucketExists(asset: Asset) : Boolean

    fun storeMetadata(asset: Asset, metadata: Map<String, Any>?)
    fun storeSourceFile(asset: Asset, stream: InputStream)
    fun getSourceFile(asset: Asset): InputStream
    fun streamSourceFile(asset: Asset, output: OutputStream)
    fun getMetadata(asset: Asset): Map<String, Any>

    fun storeFile(asset: Asset, name: String, stream: InputStream)
    fun getFile(asset: Asset, name: String): InputStream
    fun streamFile(asset: Asset, name: String, output: OutputStream)

    fun getObjectStream(url: URL): ObjectStream
}

data class ObjectStream (
        val steam: InputStream,
        val size: Long,
        val type: String?)


open class ZorroaStorageException(e: Exception) : RuntimeException(e)
class StorageWriteException (e: Exception) : ZorroaStorageException(e)
class StorageReadException (e: Exception) : ZorroaStorageException(e)


class GcpStorageService : StorageService {

    lateinit var storage: Storage

    @PostConstruct
    fun setup() {
        storage = StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream("keys/rmaas-dit1.json"))).build().service
    }
    override fun createBucket(asset: Asset) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeBucket(asset: Asset) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun bucketExists(asset: Asset): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeMetadata(asset: Asset, metadata: Map<String, Any>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSourceFile(asset: Asset, stream: InputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSourceFile(asset: Asset): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun streamSourceFile(asset: Asset, output: OutputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMetadata(asset: Asset): Map<String, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeFile(asset: Asset, name: String, stream: InputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getFile(asset: Asset, name: String): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun streamFile(asset: Asset, name: String, output: OutputStream) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getObjectStream(url: URL): ObjectStream {
        var path = url.path
        path = path.replace("rmaas-us-dit1/", "")
        val blobId = BlobId.of("rmaas-us-dit1", path.substring(1))
        val storage =  storage.get(blobId)

        return ObjectStream(
                Channels.newInputStream(storage.reader()),
                storage.size,
                storage.contentType)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(GcpStorageService::class.java)

    }
}


class MinioStorageImpl @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {

    override fun getObjectStream(url: URL): ObjectStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val client: MinioClient = MinioClient(
            properties.getString("archivist.storage.minio.endpoint"),
            properties.getString("archivist.storage.minio.accessKey"),
            properties.getString("archivist.storage.minio.secretKey"))

    override fun removeBucket(asset: Asset) {
        try {
            client.removeBucket(getBucketName(asset))
        } catch (e: MinioException) {
            throw StorageWriteException(e)
        }
    }

    override fun bucketExists(asset: Asset): Boolean {
        try {
            return client.bucketExists(getBucketName(asset))
        } catch (e: MinioException) {
            throw StorageReadException(e)
        }
    }

    override fun createBucket(asset: Asset) {
        val name = getBucketName(asset)
        try {
            if (!client.bucketExists(name)) {
                client.makeBucket(name)
            }
        } catch (e: MinioException) {
            throw StorageWriteException(e)
        }
    }

    override fun storeMetadata(asset: Asset, metadata: Map<String, Any>?) {
        val md = metadata ?: mapOf()
        client.putObject(getBucketName(asset), getFileName(asset, "metadata.json"),
                ByteArrayInputStream(Json.Mapper.writeValueAsString(md).toByteArray(Charsets.UTF_8)),
                "application/octet-stream")
    }

    override fun storeSourceFile(asset: Asset, stream: InputStream) {
        client.putObject(getBucketName(asset), getFileName(asset), stream,
                "application/octet-stream")
    }

    override fun getMetadata(asset: Asset): Map<String, Any> {
        val stream = client.getObject(getBucketName(asset),
                getFileName(asset, "metadata.json"))
        return Json.Mapper.readValue(stream, Json.GENERIC_MAP)
    }

    override fun getSourceFile(asset: Asset): InputStream {
        return client.getObject(getBucketName(asset), getFileName(asset))
    }

    override fun streamSourceFile(asset: Asset, output: OutputStream) {
        IOUtils.copy(client.getObject(getBucketName(asset), getFileName(asset)), output)
    }

    override fun storeFile(asset: Asset, name: String, stream: InputStream) {
        client.putObject(getBucketName(asset), getFileName(asset, name), stream,
                "application/octet-stream")
    }

    override fun getFile(asset: Asset, name: String): InputStream {
        return client.getObject(getBucketName(asset), getFileName(asset, name))
    }

    override fun streamFile(asset: Asset, name: String, output: OutputStream) {
        IOUtils.copy(client.getObject(getBucketName(asset), getFileName(asset, name)), output)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MinioStorageImpl::class.java)

    }
}
