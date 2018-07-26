package com.zorroa.analyst.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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
    fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray) : URL
    fun storeBlob(path: String, mediaType: String, bytes: ByteArray) : URL
    fun getSignedUrl(path: String) : URL
    fun getInputStream(path: String): InputStream
}

/**
 * A properties class injected with analyst.storage properties.
 */
@Configuration
@ConfigurationProperties("analyst.storage")
class StorageProperties {

    var type: String? = null
    var gcp: Map<String, String>? = null
}

/**
 * The LocalJobStorageServiceImpl stores job runtime data on a shared local volume,
 * similar to 0.39.
 */
class LocalJobStorageServiceImpl : JobStorageService {

    override fun storeBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSignedUrl(path: String): URL {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        return URL("http://localhost")
    }

    override fun getInputStream(path: String): InputStream {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

/**
 * A GCP implementation of the JobStorageService.
 */
class GcpStorageServiceImpl : JobStorageService {

    @Autowired
    lateinit var settings : StorageProperties

    lateinit var storage: Storage

    lateinit var bucket : String

    @PostConstruct
    fun setup() {
        bucket = settings.gcp!!.getValue("bucket")

        val creds= settings.gcp!!["credentials"]
        storage = StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream(creds))).build().service
    }

    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray) : URL {
        logger.info("Storing in bucket: {} {}", bucket, path)
        val blobId = BlobId.of(bucket, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build()
        val blob = storage.create(blobInfo, bytes)

        return blob.signUrl(60, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET))
    }

    override fun storeBlob(path: String, mediaType: String, bytes: ByteArray) : URL {
        logger.info("Storing in bucket: {} {}", bucket, path)
        val blobId = BlobId.of(bucket, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build()
        val blob = storage.create(blobInfo, bytes)
        return URL(blob.selfLink)
    }

    override fun getSignedUrl(path: String) : URL {
        logger.info("Storing in bucket: {} {}", bucket, path)
        val blobId = BlobId.of(bucket, path)
        val blobInfo = storage.get(blobId)
        return blobInfo.signUrl(60, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET))
    }

    override fun getInputStream(path: String): InputStream {
        val blobId = BlobId.of(bucket, path)
        return Channels.newInputStream(storage.get(blobId).reader())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpStorageServiceImpl::class.java)
    }
}
