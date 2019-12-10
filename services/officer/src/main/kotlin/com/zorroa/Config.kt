package com.zorroa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import org.eclipse.jetty.server.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths

object Config {

    class BucketConfiguration(
        val url: String = System.getenv("MLSTORAGE_URL") ?: "http://minio:9000",
        val name: String = System.getenv("MLSTORAGE_BUCKET") ?: "ml-storage",
        val accessKey: String = System.getenv("MLSTORAGE_ACCESSKEY") ?: "the_access_key",
        val secretKey: String = System.getenv("MLSTORAGE_SECRETKEY") ?: "the_secret_key"
    )

    class OfficerConfiguration(
        val port: Int = (System.getenv("OFFICER_PORT") ?: "7078").toInt(),
        val loadMultiplier: Int = (System.getenv("OFFICER_LOADMULTIPLIER") ?: "2").toInt()
    )

    val logger: Logger = LoggerFactory.getLogger(StorageManager::class.java)

    private val mapper = ObjectMapper(YAMLFactory())
    private val configFilePath = Paths.get(ServerOptions.configFile)

    val officer : OfficerConfiguration
    val bucket : BucketConfiguration

    init {
        bucket = BucketConfiguration()
        officer = OfficerConfiguration()
    }
}