package com.zorroa.analyst.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.io.InputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


/**
 * JobStorageService is for storing job runtime data, like the ZPS script, or anything
 * else needed by the job.
 *
 * The storage service is also responsible for clearing out expired paths.
 */
interface JobStorageService {
    fun storeSignedBlob(bucket: String, path: String, mediaType: String, bytes: ByteArray) : URL
    fun storeBlob(bucket: String, path: String, mediaType: String, bytes: ByteArray) : URL
    fun getSignedUrl(bucket: String, path: String) : URL
    fun getInputStream(bucket: String, path: String): InputStream
}

/**
 * A properties class injected with analyst.storage properties.
 */
@Configuration
@ConfigurationProperties("analyst.storage")
class StorageProperties {

    var type: String? = null
}

/**
 * The LocalJobStorageServiceImpl stores job runtime data on a shared local volume,
 * similar to 0.39.
 */
class LocalJobStorageServiceImpl : JobStorageService {

    override fun storeBlob(bucket: String, path: String, mediaType: String, bytes: ByteArray): URL {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSignedUrl(bucket: String, path: String): URL {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSignedBlob(bucket: String, path: String, mediaType: String, bytes: ByteArray): URL {
        return URL("http://localhost")
    }

    override fun getInputStream(bucket: String, path: String): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

/**
 * A GCP implementation of the JobStorageService.
 */
class GcpStorageServiceImpl : JobStorageService {

    lateinit var storage: Storage

    lateinit var bucket : String

    @Value("\${analyst.config.path}")
    lateinit var configPath: String

    @PostConstruct
    fun setup() {
        storage = StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream("$configPath/data-credentials.json"))).build().service
    }

    override fun storeSignedBlob(bucket: String, path: String, mediaType: String, bytes: ByteArray) : URL {
        logger.info("Storing in bucket: {} {}", bucket, path)
        val blobId = BlobId.of(bucket, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build()
        val blob = storage.create(blobInfo, bytes)

        return blob.signUrl(60, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET))
    }

    override fun storeBlob(bucket: String, path: String, mediaType: String, bytes: ByteArray) : URL {
        logger.info("Storing in bucket: {} {}", bucket, path)
        val blobId = BlobId.of(bucket, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build()
        val blob = storage.create(blobInfo, bytes)
        return URL(blob.selfLink)
    }

    override fun getSignedUrl(bucket: String, path: String) : URL {
        logger.info("Storing in bucket: {} {}", bucket, path)
        val blobId = BlobId.of(bucket, path)
        val blobInfo = storage.get(blobId)
        return blobInfo.signUrl(60, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET))
    }

    override fun getInputStream(bucket: String, path: String): InputStream {
        val blobId = BlobId.of(bucket, path)
        return Channels.newInputStream(storage.get(blobId).reader())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpStorageServiceImpl::class.java)
    }
}
