package com.zorroa

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageException
import com.google.cloud.storage.StorageOptions
import io.minio.MinioClient
import io.minio.errors.ErrorResponseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream

object StorageManager {

    private var storageClient: StorageClient = when (Config.storageClient) {
        "minio" -> MinioStorageClient()
        "gcs" -> GcsStorageClient()
        else -> MinioStorageClient()
    }

    fun storageClient(): StorageClient {
        return storageClient
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

open class MinioStorageClient : StorageClient {

    val logger: Logger = LoggerFactory.getLogger(MinioStorageClient::class.java)

    val minioClient = MinioClient(
        Config.bucket.url,
        Config.bucket.accessKey,
        Config.bucket.secretKey
    )
    private val bucket = Config.bucket.name

    init {
        logger.info("Initializing ML Storage: {}", Config.bucket.url)
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

    override fun store(path: String, inputStream: InputStream, size: Long, fileType: String) {
        logger.info("Storing: {} {}", bucket, path.removePrefix("/"))
        minioClient.putObject(
            bucket, path.removePrefix("/"),
            inputStream,
            size,
            null,
            null,
            fileType
        )
    }

    override fun fetch(path: String): InputStream {
        return minioClient.getObject(Config.bucket.name, path)
    }

    override fun bucketExists(path: String): Boolean {
        return minioClient.bucketExists(bucket)
    }

    override fun exists(path: String): Boolean {
        return try {
            minioClient.statObject(Config.bucket.name, path.removePrefix("/"))
            true
        } catch (e: ErrorResponseException) {
            IOHandler.logger.warn("Object does not exist: {} / {}", Config.bucket.name, path)
            false
        }
    }

    override fun delete(path: String) {
        minioClient.removeObject(Config.bucket.name, path)
    }

    override fun bucket(): String {
        return bucket
    }
}

open class GcsStorageClient(bucketName: String? = null) : StorageClient {

    val gcs: Storage = StorageOptions.getDefaultInstance().service
    private val bucket = bucketName ?: Config.bucket.name

    override fun store(path: String, inputStream: InputStream, size: Long, fileType: String) {
        val blobId = getBlobId(path.removePrefix("/"))
        val info = BlobInfo.newBuilder(blobId)
        info.setContentType(fileType)
        gcs.create(info.build(), inputStream.readBytes())
    }

    override fun fetch(path: String): InputStream {
        val blobId = getBlobId(path.removePrefix("/"))
        val blob = gcs.get(blobId)
        return blob.getContent().inputStream()
    }

    override fun bucketExists(path: String): Boolean {
        try {
            return gcs.get(path).exists()
        } catch (ex: StorageException) {
            logger.warn("Bucket $path does not exists")
        }
        return false
    }

    override fun exists(path: String): Boolean {
        return try {
            val blob = gcs.get(getBlobId(path))
            blob?.exists() ?: false
        } catch (e: StorageException) {
            logger.warn("Failed to access $path to check exists", e)
            false
        }
    }

    override fun delete(path: String) {
        gcs.delete(getBlobId(path))
    }

    override fun bucket(): String {
        return bucket
    }

    fun getBlobId(path: String): BlobId {
        return BlobId.of(bucket, path.removePrefix("/"))
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcsStorageClient::class.java)
    }
}
