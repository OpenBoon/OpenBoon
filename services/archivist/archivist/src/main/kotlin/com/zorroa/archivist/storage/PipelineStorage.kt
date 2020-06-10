package com.zorroa.archivist.storage

import io.minio.MinioClient
import io.minio.errors.ErrorResponseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

/**
 * The PipelineStorage tier provides the Pipeline with the ability to store
 * and retrieve analysis runtime data.
 */
interface PipelineStorage

@Configuration
@ConfigurationProperties("zmlp.storage.pipeline")
class PipelineStorageConfiguration {
    lateinit var bucket: String
    lateinit var accessKey: String
    lateinit var secretKey: String
    lateinit var url: String
}

@Service
class PipelineStorageImpl(
    val config: PipelineStorageConfiguration
) : PipelineStorage {

    val client: MinioClient

    init {
        logger.info("Initializing shared storage: url='${config.url}' bucket='${config.bucket}'")
        client = MinioClient(config.url, config.accessKey, config.secretKey)
    }

    @PostConstruct
    fun createBucket() {

        if (!client.bucketExists(config.bucket)) {
            try {
                client.makeBucket(config.bucket)
            } catch (e: ErrorResponseException) {
                // Handle race condition where 2 things make the bucket.
                if (e.errorResponse().code() != "BucketAlreadyOwnedByYou") {
                    throw e
                }
            }
        }

        client.setBucketLifeCycle(config.bucket, LIFECYCLE)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PipelineStorageImpl::class.java)

        // Setup the tmp-files lifecycle
        const val LIFECYCLE =
            """<LifecycleConfiguration><Rule>
                    <ID>temp-file-lifecycle</ID>
                    <Filter><Prefix>tmp-files/</Prefix></Filter>
                    <Status>Enabled</Status>
                    <Expiration><Days>1</Days></Expiration>
                    </Rule>
                    </LifecycleConfiguration>"""
    }
}
