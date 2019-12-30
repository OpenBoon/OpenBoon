package com.zorroa

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Config {

    class BucketConfiguration(
        val url: String = System.getenv("ZMLP_ISTORAGE_URL") ?: "http://localhost:9000",
        val name: String = System.getenv("ZMLP_ISTORAGE_BUCKET") ?: "istorage",
        val accessKey: String = System.getenv("ZMLP_ISTORAGE_ACCESSKEY") ?: "qwerty123",
        val secretKey: String = System.getenv("ZMLP_ISTORAGE_SECRETKEY") ?: "123qwerty"
    )

    class OfficerConfiguration(
        val port: Int = (System.getenv("OFFICER_PORT") ?: "7078").toInt(),
        val loadMultiplier: Int = (System.getenv("OFFICER_LOADMULTIPLIER") ?: "2").toInt()
    )

    val logger: Logger = LoggerFactory.getLogger(Config::class.java)

    val officer: OfficerConfiguration
    val bucket: BucketConfiguration

    init {
        bucket = BucketConfiguration()
        officer = OfficerConfiguration()
    }

    fun logSystemConfiguration() {
        val heapSize = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val maxHeapSize = Runtime.getRuntime().maxMemory() / 1024 / 1024

        logger.info("Java heap size: ${heapSize}m")
        logger.info("Java max heap size: ${maxHeapSize}m")
    }
}
