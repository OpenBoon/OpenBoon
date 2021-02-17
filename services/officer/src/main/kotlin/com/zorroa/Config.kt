package com.zorroa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Config {

    open class BucketConfiguration(
        val name: String,
        val accessKey: String?,
        val secretKey: String?,
        val url: String?,
    )

    class MinioBucketConfiguration() : BucketConfiguration(
        System.getenv("BOONAI_STORAGE_PROJECT_BUCKET") ?: "project-storage",
        System.getenv("BOONAI_STORAGE_PROJECT_ACCESSKEY") ?: "qwerty123",
        System.getenv("BOONAI_STORAGE_PROJECT_SECRETKEY") ?: "123qwerty",
        System.getenv("BOONAI_STORAGE_PROJECT_URL") ?: "http://localhost:9000",
    )

    /**
     * We don't need anything besides bucket name for GCS.
     */
    class GcsBucketConfiguration() : BucketConfiguration(
        System.getenv("BOONAI_STORAGE_PROJECT_BUCKET") ?: "project-storage",
        null,
        null,
        null
    )

    class OfficerConfiguration(
        val port: Int = (System.getenv("OFFICER_PORT") ?: "7078").toInt(),
        val loadMultiplier: Int = (System.getenv("OFFICER_LOADMULTIPLIER") ?: "2").toInt(),
    )

    val logger: Logger = LoggerFactory.getLogger(Config::class.java)

    val officer: OfficerConfiguration
    val bucket: BucketConfiguration
    val storageClient = System.getenv("BOONAI_STORAGE_CLIENT")
        ?: System.getProperty("BOONAI_STORAGE_CLIENT") ?: "minio"

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
