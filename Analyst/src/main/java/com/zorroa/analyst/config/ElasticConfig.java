package com.zorroa.analyst.config;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
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

    @Bean
    public Client elastic() throws IOException {

        Random rand = new Random(System.nanoTime());
        int number = rand.nextInt(99999);

        String hostName = InetAddress.getLocalHost().getHostName();
        String nodeName = String.format("%s_%05d", hostName, number);

        ImmutableSettings.Builder builder =
                ImmutableSettings.settingsBuilder()
                        .put("cluster.name", "zorroa")
                        .put("node.name", nodeName)
                        .put("node.master", true)
                        .put("script.native.archivistDate.type", "com.zorroa.common.elastic.ArchivistDateScriptFactory")
                        .put("cluster.routing.allocation.disk.threshold_enabled", false)
                        .put("script.indexed", true)
                        .put("script.update", true)
                        .put("script.engine.groovy.indexed.update", true);

        Node node = nodeBuilder()
                .data(true)
                .settings(builder.build())
                .node();

        return node.client();
    }
}
