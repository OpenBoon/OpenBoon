package com.zorroa

import io.minio.MinioClient
import io.minio.errors.ErrorResponseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

object StorageManager {

    private var minioStorageClient: MinioStorageClient = MinioStorageClient()

    fun storageClient(): StorageClient {
        // Get right storage from Env and return
        return minioStorageClient
    }
}

interface StorageClient {

    fun store(path: String, inputStream: InputStream, size: Long, fileType: String)

    fun fetch(path: String): InputStream

    fun bucketExists(path: String): Boolean

    fun exists(path: String): Boolean

    fun delete(path: String)

    fun bucket(): String
}

class MinioStorageClient : StorageClient {

    val logger: Logger = LoggerFactory.getLogger(MinioStorageClient::class.java)

    val minioClient = MinioClient(
        Config.minioBucket.url,
        Config.minioBucket.accessKey,
        Config.minioBucket.secretKey
    )
    val bucket = Config.minioBucket.name

    init {
        logger.info("Initializing ML Storage: {}", Config.minioBucket.url)
        createBucket()
    }

    private fun createBucket() {
        if (!minioClient.bucketExists(Config.minioBucket.name)) {
            try {
                minioClient.makeBucket(Config.minioBucket.name)
            } catch (e: ErrorResponseException) {
                // Handle race condition where 2 things make the bucket.
                if (e.errorResponse().code() != "BucketAlreadyOwnedByYou") {
                    throw e
                }
            }
        }
    }

    override fun store(path: String, inputStream: InputStream, size: Long, fileType: String) {
        minioClient.putObject(
            bucket, path,
            inputStream,
            size,
            null,
            null,
            fileType
        )
    }

    override fun fetch(path: String): InputStream {
        return minioClient.getObject(Config.minioBucket.name, path)
    }

    override fun bucketExists(path: String): Boolean {
        return minioClient.bucketExists(bucket)
    }

    override fun exists(path: String): Boolean {
        return try {
            minioClient.statObject(Config.minioBucket.name, path)
            true
        } catch (e: ErrorResponseException) {
            IOHandler.logger.warn("Object does not exist: {}", path)
            false
        }
    }

    override fun delete(path: String) {
        minioClient.removeObject(Config.minioBucket.name, path)
    }

    override fun bucket(): String {
        return bucket
    }
}

/**
 * A ByteArrayOutputStream which provides an efficient way to
 * obtain a BufferedInputStream without copying the internal byte buffer.
 */
class ReversibleByteArrayOutputStream(size: Int = 2048) : ByteArrayOutputStream(size) {

    fun toInputStream(): InputStream {
        return BufferedInputStream(ByteArrayInputStream(buf, 0, count))
    }
}

/**
 * Manages render inputs and outputs.
 */
class IOHandler(val options: RenderRequest) {

    fun writeImage(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.storageClient().store(
            getImagePath(page),
            outputStream.toInputStream(),
            outputStream.size().toLong(),
            "image/jpeg"
        )
    }

    fun writeMetadata(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.storageClient().store(
            getMetadataPath(page),
            outputStream.toInputStream(),
            outputStream.size().toLong(),
            "application/json"
        )
    }

    fun getImagePath(page: Int): String {
        return "$PREFIX/${options.outputDir}/proxy.$page.jpg"
    }

    fun getMetadataPath(page: Int): String {
        return "$PREFIX/${options.outputDir}/metadata.$page.json"
    }

    fun getOutputUri(): String {
        return "zmlp://${Config.minioBucket.name}/$PREFIX/${options.outputDir}"
    }

    fun getMetadata(page: Int = 1): InputStream {
        return StorageManager.storageClient().fetch(getMetadataPath(page))
    }

    fun getImage(page: Int = 1): InputStream {
        return StorageManager.storageClient().fetch(getImagePath(page))
    }

    fun exists(page: Int = 1): Boolean {
        val path = getMetadataPath(page)
        logger.info("Checking path: {}", path)
        return StorageManager.storageClient().exists(path)
    }

    fun removeImage(page: Int = 1) {
        StorageManager.storageClient().delete(getImagePath(page))
    }

    fun removeMetadata(page: Int = 1) {
        StorageManager.storageClient().delete(getMetadataPath(page))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IOHandler::class.java)

        // The size of the pre-allocated by array for images.
        val IMG_BUFFER_SIZE = 65536

        // The object path prefix
        val PREFIX = "tmp-files/officer"
    }
}
