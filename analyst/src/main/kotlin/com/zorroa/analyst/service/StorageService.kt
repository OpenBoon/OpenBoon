package com.zorroa.analyst.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


interface StorageService {
    fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray) : URL
}

@Configuration
@ConfigurationProperties("analyst.storage")
class StorageProperties {

    var type: String? = null
    var gcp: Map<String, String>? = null
}

class LocalStorageServiceImpl : StorageService {
    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        return URL("http://localhost")
    }
}

/**
 * A GCP implemention of the StorageService which may be replaced with a CDV
 * implementation.
 */
class GcpStorageServiceImpl : StorageService {

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


    companion object {
        private val logger = LoggerFactory.getLogger(GcpStorageServiceImpl::class.java)
    }
}
