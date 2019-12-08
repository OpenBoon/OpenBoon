package com.zorroa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.eclipse.jetty.server.Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths

object Config {

    class BucketConfiguration(
        val endpoint: String,
        val name: String,
        val accessKey: String,
        val secretKey: String,
        val retentionHours: Int
    )

    class OfficerConfiguration(
        val bucket : BucketConfiguration,
        val verbose: Boolean,
        val port: Int,
        val loadMultiplier: Int
    )

    val logger: Logger = LoggerFactory.getLogger(StorageManager::class.java)

    private val mapper = ObjectMapper(YAMLFactory())
    private val configFilePath = Paths.get(ServerOptions.configFile)

    val main : OfficerConfiguration
    val bucket : BucketConfiguration

    init {
        logger.info("Initializing config: $configFilePath")
        mapper.findAndRegisterModules()
        mapper.propertyNamingStrategy = PropertyNamingStrategy.KEBAB_CASE

        main = mapper.readValue(configFilePath.toFile(), OfficerConfiguration::class.java)
        bucket = main.bucket

    }
}