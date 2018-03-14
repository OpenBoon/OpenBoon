package com.zorroa.analyst.config

import com.zorroa.analyst.isUnitTest
import com.zorroa.common.elastic.ElasticClientUtils
import com.zorroa.sdk.util.FileUtils
import org.elasticsearch.client.Client
import com.zorroa.archivist.sdk.config.ApplicationProperties
import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException
import java.net.InetAddress
import java.util.*

@Configuration
class ElasticConfig @Autowired constructor(
        private val properties: ApplicationProperties) {

    @Bean
    @Throws(IOException::class)
    fun elastic(): Client? {
        /**
         * If we're not master or data, the no need to even start elastic.
         */
        if (!properties.getBoolean("analyst.index.data") && !properties.getBoolean("analyst.index.master")) {
            return null
        }

        val rand = Random(System.nanoTime())
        val number = rand.nextInt(99999)

        val hostName = InetAddress.getLocalHost().hostName
        val nodeName = String.format("%s_%05d", hostName, number)
        val elasticMaster = properties.getString("analyst.index.host")

        val builder = Settings.settingsBuilder()
                .put("cluster.name", "zorroa")
                .put("path.data", properties.getString("analyst.path.index"))
                .put("path.home", properties.getString("analyst.path.home"))
                .put("node.name", nodeName)
                .put("node.master", properties.getBoolean("analyst.index.master"))
                .put("node.data", properties.getBoolean("analyst.index.data"))
                .put("cluster.routing.allocation.disk.threshold_enabled", false)
                .put("http.enabled", "false")
                .put("network.host", "0.0.0.0")
                .put("discovery.zen.no_master_block", "write")
                .put("discovery.zen.fd.ping_timeout", "3s")
                .put("discovery.zen.fd.ping_retries", 10)
                .put("discovery.zen.ping.multicast.enabled", false)
                .putArray("discovery.zen.ping.unicast.hosts", elasticMaster)
                .put("action.auto_create_index", "-archivist*")

        if (properties.getBoolean("analyst.maintenance.backups.enabled")) {
            val path = FileUtils.normalize(properties.getPath("analyst.path.backups").resolve("index"))
            builder.putArray("path.repo", path.toString())
        }

        if (isUnitTest) {
            logger.info("Elastic in unit test mode")
            builder.put("node.master", true)
            builder.put("index.refresh_interval", "1s")
            builder.put("index.translog.disable_flush", false)
            builder.put("node.local", true)
        } else {
            logger.info("Connecting to elastic master: {}", elasticMaster)
        }

        return ElasticClientUtils.initializeClient(builder)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ElasticConfig::class.java)
    }
}
