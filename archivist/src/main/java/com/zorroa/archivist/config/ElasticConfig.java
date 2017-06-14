package com.zorroa.archivist.config;

import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.util.FileUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;

/**
 * Created by chambers on 8/10/16.
 */
@Configuration
public class ElasticConfig {

    private static final Logger logger = LoggerFactory.getLogger(ElasticConfig.class);

    @Autowired
    ApplicationProperties properties;

    private String nodeName;
    private String hostName;

    @PostConstruct
    public void init() throws UnknownHostException {
        hostName = InetAddress.getLocalHost().getHostName();
        nodeName = String.format("%s_master", hostName);
    }

    @Bean
    public Client elastic() throws IOException {

        org.elasticsearch.common.settings.Settings.Builder builder =
                Settings.settingsBuilder()
                        .put("path.data", properties.getString("archivist.path.index"))
                        .put("path.home", properties.getString("archivist.path.home"))
                        .put("cluster.name", "zorroa")
                        .put("node.name", nodeName)
                        .put("client.transport.sniff", true)
                        .put("transport.tcp.port", properties.getInt("zorroa.cluster.index.port"))
                        .put("network.host", "0.0.0.0")
                        .put("discovery.zen.ping.multicast.enabled", false)
                        .put("discovery.zen.fd.ping_timeout", "3s")
                        .put("discovery.zen.fd.ping_retries", 10)
                        .put("script.engine.groovy.indexed.update", true)
                        .put("node.data", properties.getBoolean("archivist.index.data"))
                        .put("node.master", properties.getBoolean("archivist.index.master"))
                        .put("path.plugins", "{path.home}/es-plugins")
                        .put("action.auto_create_index",  "-arch*,+.scripts,-*");

        if (properties.getBoolean("archivist.maintenance.backups.enabled")) {
            Path path = FileUtils.normalize(properties.getPath("archivist.path.backups").resolve("index"));
            builder.putArray("path.repo", path.toString());
        }

        if (ArchivistConfiguration.unittest) {
            builder.put("index.refresh_interval", "1s");
            builder.put("index.translog.disable_flush", true);
            builder.put("node.local", true);
        }

        return ElasticClientUtils.initializeClient(builder);
    }

}
