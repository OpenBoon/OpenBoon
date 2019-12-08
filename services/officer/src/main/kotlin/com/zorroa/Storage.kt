package com.zorroa

import io.minio.MinioClient
import org.apache.tika.Tika
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

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
            minioClient.makeBucket(Config.bucket.name)
        }

        val lifeCycle = """<LifecycleConfiguration><Rule>
                    <ID>expire-officer-files</ID>
                    <Prefix>officer/</Prefix>
                    <Status>Enabled</Status>
                    <Expiration><Minutes>1</Minutes></Expiration>
                    </Rule>
                    </LifecycleConfiguration>"""

        minioClient.setBucketLifeCycle(bucket, lifeCycle)
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

    /**
     * Keep track of any temp files we create so they
     * can be removed later.
     */
    private val tempFiles = mutableSetOf<Path>()

    val tika = Tika()

    fun writeImage(page: Int, outputStream: ReversibleByteArrayOutputStream) {
        StorageManager.minioClient.putObject(
            StorageManager.bucket, getImagePath(page),
            outputStream.toInputStream(), outputStream.size().toLong(), null, null,
            tika.detect(options.fileName)
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
        return "officer/${options.outputDir}/proxy.$page.jpg"
    }

    fun getMetadataPath(page: Int): String {
        return "officer/${options.outputDir}/metadata.$page.json"
    }

    fun getOutputUri(): String {
        return "pixml://${Config.bucket.name}/officer/${options.outputDir}"
    }

    fun getMetadata(page: Int = 1): InputStream {
        return StorageManager.minioClient.getObject(Config.bucket.name, getMetadataPath(page))
    }

    fun getImage(page: Int = 1): InputStream {
        return StorageManager.minioClient.getObject(Config.bucket.name, getImagePath(page))
    }

    fun removeImage(page: Int = 1) {
        StorageManager.minioClient.removeObject(Config.bucket.name, getImagePath(page))
    }

    fun removeMetadata(page: Int = 1) {
        StorageManager.minioClient.removeObject(Config.bucket.name, getMetadataPath(page))
    }

    fun removeTempFiles() {
        tempFiles.forEach {
            try {
                logger.info("removing temp $it")
                Files.delete(it)
            } catch (e: IOException) {
                logger.warn("Failed to delete temp file: $it", e)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(IOHandler::class.java)
    }
}
