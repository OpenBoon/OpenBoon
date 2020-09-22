package com.zorroa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Config {

    open class BucketConfiguration(
        val url: String,
        val name: String,
        val accessKey: String?,
        val secretKey: String?,
        val credentialsPath: String?
    )

    class MinioBucketConfiguration(
    ) : BucketConfiguration(
        System.getenv("ZMLP_STORAGE_PIPELINE_URL") ?: "http://localhost:9000",
        System.getenv("ZMLP_STORAGE_PIPELINE_BUCKET") ?: "pipeline-storage",
        System.getenv("ZMLP_STORAGE_PIPELINE_ACCESSKEY") ?: "qwerty123",
        System.getenv("ZMLP_STORAGE_PIPELINE_SECRETKEY") ?: "123qwerty",
        null
    )

    class GcsBucketConfiguration(
    ) : BucketConfiguration(
        System.getenv("ZMLP_STORAGE_PIPELINE_URL") ?: "http://localhost:9000",
        System.getenv("ZMLP_STORAGE_PIPELINE_BUCKET") ?: "pipeline-storage",
        null,
        null,
        System.getenv("CREDENTIALS_PATH") ?: "/secrets/gcs/credentials.json"
    )

    class OfficerConfiguration(
        val port: Int = (System.getenv("OFFICER_PORT") ?: "7078").toInt(),
        val loadMultiplier: Int = (System.getenv("OFFICER_LOADMULTIPLIER") ?: "2").toInt()
    )

    val logger: Logger = LoggerFactory.getLogger(Config::class.java)

    val officer: OfficerConfiguration
    val bucket: BucketConfiguration
    val storageClient = System.getenv("STORAGE_CLIENT") ?: "minio"

    init {
        officer = OfficerConfiguration()

        bucket = when (storageClient) {
            "minio" -> MinioBucketConfiguration()
            "gcs" -> GcsBucketConfiguration()
            else -> MinioBucketConfiguration()
        }
    }

    fun logSystemConfiguration() {
        val heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val maxHeapSize = Runtime.getRuntime().maxMemory() / 1024 / 1024

        logger.info("Java heap size: ${heapSize}m")
        logger.info("Java max heap size: ${maxHeapSize}m")
    }
}
