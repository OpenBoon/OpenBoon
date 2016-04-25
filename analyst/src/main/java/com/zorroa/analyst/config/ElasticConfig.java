package com.zorroa.analyst.config;

import com.zorroa.analyst.Application;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.common.elastic.ElasticClientUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Random;

/**
 * Created by chambers on 2/12/16.
 */
@Configuration
public class ElasticConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticConfig.class);

    @Autowired
    ApplicationProperties properties;

    @Bean
    public Client elastic() throws IOException {

        Random rand = new Random(System.nanoTime());
        int number = rand.nextInt(99999);

        String hostName = InetAddress.getLocalHost().getHostName();
        String nodeName = String.format("%s_%05d", hostName, number);
        String archivistHost = String.format("%s:%d",
                URI.create(properties.getString("analyst.master.host")).getHost(),
                properties.getInt("zorroa.common.index.port"));

        Settings.Builder builder =
                Settings.settingsBuilder()
                        .put("cluster.name", "zorroa")
                        .put("path.data", properties.getString("analyst.path.index"))
                        .put("path.home", properties.getString("analyst.path.home"))
                        .put("node.name", nodeName)
                        .put("node.master", false)
                        .put("cluster.routing.allocation.disk.threshold_enabled", false)
                        .put("http.enabled", "false")
                        .put("discovery.zen.no_master_block", "write")
                        .put("discovery.zen.fd.ping_timeout", "3s")
                        .put("discovery.zen.fd.ping_retries", 10)
                        .put("discovery.zen.ping.multicast.enabled", false)
                        .putArray("discovery.zen.ping.unicast.hosts", archivistHost)
                        .put("node.data", properties.getBoolean("analyst.index.data"));

        if (Application.isUnitTest()) {
            logger.info("Elastic in unit test mode");
            builder.put("node.master", true);
            builder.put("index.refresh_interval", "1s");
            builder.put("index.translog.disable_flush", false);
            builder.put("node.local", true);
        }
        else {
            logger.info("Connecting to elastic master: {}", archivistHost);
        }

        return ElasticClientUtils.initializeClient(builder);
    }
}
