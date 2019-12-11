package com.zorroa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.nio.file.Paths
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Config {

    class BucketConfiguration(
        val url: String = System.getenv("MLSTORAGE_URL") ?: "http://localhost:9000",
        val name: String = System.getenv("MLSTORAGE_BUCKET") ?: "ml-storage",
        val accessKey: String = System.getenv("MLSTORAGE_ACCESSKEY") ?: "qwerty123",
        val secretKey: String = System.getenv("MLSTORAGE_SECRETKEY") ?: "123qwerty"
    )

    class OfficerConfiguration(
        val port: Int = (System.getenv("OFFICER_PORT") ?: "7078").toInt(),
        val loadMultiplier: Int = (System.getenv("OFFICER_LOADMULTIPLIER") ?: "2").toInt()
    )

    val logger: Logger = LoggerFactory.getLogger(StorageManager::class.java)

    private val mapper = ObjectMapper(YAMLFactory())
    private val configFilePath = Paths.get(ServerOptions.configFile)

    val officer: OfficerConfiguration
    val bucket: BucketConfiguration

    init {
        bucket = BucketConfiguration()
        officer = OfficerConfiguration()
    }
}
