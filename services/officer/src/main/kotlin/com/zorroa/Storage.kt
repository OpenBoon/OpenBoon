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

    val logger: Logger = LoggerFactory.getLogger(StorageManager::class.java)

    val minioClient = MinioClient(
        Config.bucket.endpoint,
        Config.bucket.accessKey,
        Config.bucket.secretKey
    )
    val bucket = Config.bucket.name

    init {
        createBucket()
    }

    private fun createBucket() {
        if (!minioClient.bucketExists(Config.bucket.name)) {
            try {
                minioClient.makeBucket(Config.bucket.name)
            } catch (e: ErrorResponseException) {
                // Handle race condition where 2 things make the bucket.
                if (e.errorResponse().code() != "BucketAlreadyOwnedByYou") {
                    throw e
                }
            }
        }
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
class IOHandler(val options: Options) {

    fun writeImage(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.minioClient.putObject(
            StorageManager.bucket, getImagePath(page),
            outputStream.toInputStream(), outputStream.size().toLong(), null, null,
            "image/jpeg"
        )
    }

    fun writeMetadata(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.minioClient.putObject(
            StorageManager.bucket, getMetadataPath(page),
            outputStream.toInputStream(), outputStream.size().toLong(), null, null,
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
        return "pixml://${Config.bucket.name}/$PREFIX/${options.outputDir}"
    }

    fun getMetadata(page: Int = 1): InputStream {
        return StorageManager.minioClient.getObject(Config.bucket.name, getMetadataPath(page))
    }

    fun getImage(page: Int = 1): InputStream {
        return StorageManager.minioClient.getObject(Config.bucket.name, getImagePath(page))
    }

    fun exists(page: Int = 1): Boolean {
        val path = getMetadataPath(page)
        return try {
            StorageManager.minioClient.statObject(Config.bucket.name, path)
            true
        } catch (e: ErrorResponseException) {
            logger.info("Object does not exist: {}", path)
            false
        }
    }

    fun removeImage(page: Int = 1) {
        StorageManager.minioClient.removeObject(Config.bucket.name, getImagePath(page))
    }

    fun removeMetadata(page: Int = 1) {
        StorageManager.minioClient.removeObject(Config.bucket.name, getMetadataPath(page))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IOHandler::class.java)

        // The size of the pre-allocated by array for images.
        val IMG_BUFFER_SIZE = 65536

        // The object path prefix
        val PREFIX = "tmp-files/officer"
    }
}
