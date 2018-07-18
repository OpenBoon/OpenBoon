package com.zorroa.irm.studio.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct


interface StorageService {
    fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray) : URL
}

@Configuration
@ConfigurationProperties("gcp.storage")
class StorageConfiguration {

    var bucket: String? = null
    var credentialsFile: String? = null
}

/**
 * A GCP implemention of the StorageService which may be replaced with a CDV
 * implementation.
 */
@Service
class GcpStorageServiceImpl : StorageService {

    @Autowired
    lateinit var settings : StorageConfiguration

    lateinit var storage: Storage

    @PostConstruct
    fun setup() {
        storage = StorageOptions.newBuilder().setCredentials(
                GoogleCredentials.fromStream(FileInputStream("config/credentials.json"))).build().service
    }

    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray) : URL {
        logger.info("Storing in bucket: {} {}", settings.bucket, path)
        val blobId = BlobId.of(settings.bucket, path)
        val blobInfo = BlobInfo.newBuilder(blobId).setContentType(mediaType).build()
        val blob = storage.create(blobInfo, bytes)

        return blob.signUrl(60, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET))
    }


    companion object {
        private val logger = LoggerFactory.getLogger(GcpStorageServiceImpl::class.java)
    }
}
