package com.zorroa

import io.minio.MinioClient
import io.minio.errors.ErrorResponseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

