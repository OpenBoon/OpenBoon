package com.zorroa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Config {

    class MinioBucketConfiguration(
        val url: String = System.getenv("ZMLP_STORAGE_PIPELINE_URL") ?: "http://localhost:9000",
        val name: String = System.getenv("ZMLP_STORAGE_PIPELINE_BUCKET") ?: "pipeline-storage",
        val accessKey: String = System.getenv("ZMLP_STORAGE_PIPELINE_ACCESSKEY") ?: "qwerty123",
        val secretKey: String = System.getenv("ZMLP_STORAGE_PIPELINE_SECRETKEY") ?: "123qwerty"
    )

    class OfficerConfiguration(
        val port: Int = (System.getenv("OFFICER_PORT") ?: "7078").toInt(),
        val loadMultiplier: Int = (System.getenv("OFFICER_LOADMULTIPLIER") ?: "2").toInt()
    )

    val logger: Logger = LoggerFactory.getLogger(Config::class.java)

    val officer: OfficerConfiguration
    val minioBucket: MinioBucketConfiguration

    init {
        minioBucket = MinioBucketConfiguration()
        officer = OfficerConfiguration()
    }

    fun logSystemConfiguration() {
        val heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val maxHeapSize = Runtime.getRuntime().maxMemory() / 1024 / 1024

        logger.info("Java heap size: ${heapSize}m")
        logger.info("Java max heap size: ${maxHeapSize}m")
    }
}
