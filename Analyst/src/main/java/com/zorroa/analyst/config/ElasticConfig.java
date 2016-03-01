package com.zorroa.analyst.config;

import com.zorroa.analyst.Application;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Created by chambers on 2/12/16.
 */
@Configuration
public class ElasticConfig {

    @Autowired
    ApplicationProperties properties;

    @Bean
    public Client elastic() throws IOException {

        Random rand = new Random(System.nanoTime());
        int number = rand.nextInt(99999);

        String hostName = InetAddress.getLocalHost().getHostName();
        String nodeName = String.format("%s_%05d", hostName, number);

        ImmutableSettings.Builder builder =
                ImmutableSettings.settingsBuilder()
                        .put("cluster.name", "zorroa")
                        .put("path.data", properties.getString("analyst.path.index"))
                        .put("node.name", nodeName)
                        .put("node.master", true)
                        .put("script.native.archivistDate.type", "com.zorroa.common.elastic.ArchivistDateScriptFactory")
                        .put("cluster.routing.allocation.disk.threshold_enabled", false)
                        .put("script.indexed", true)
                        .put("script.update", true)
                        .put("script.engine.groovy.indexed.update", true);

        if (Application.isUnitTest()) {
            builder.put("index.refresh_interval", "1s");
            builder.put("index.translog.disable_flush", true);
        }

        Node node = nodeBuilder()
                .data(true)
                .local(Application.isUnitTest())
                .settings(builder.build())
                .node();

        return node.client();
    }
}
