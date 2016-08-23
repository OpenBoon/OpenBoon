package com.zorroa.common;

import com.google.common.collect.ImmutableSet;
import com.zorroa.common.config.SpringApplicationProperties;
import com.zorroa.common.elastic.ArchivistDateScriptPlugin;
import com.zorroa.common.elastic.ZorroaNode;
import com.zorroa.sdk.config.ApplicationProperties;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;

/**
 * Created by chambers on 2/17/16.
 */
@Configuration
public class UnitTestConfiguration {

    @Bean
    public SpringApplicationProperties properties() {
        return new SpringApplicationProperties();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    @Autowired
    public Client elastic(ApplicationProperties properties) throws IOException {

        org.elasticsearch.common.settings.Settings.Builder builder =
                Settings.settingsBuilder()
                        .put("path.home", properties.getString("zorroa.cluster.index.path.home"))
                        .put("cluster.name", "zorroa")
                        .put("node.name", "common")
                        .put("client.transport.sniff", true)
                        .put("transport.tcp.port", properties.getInt("zorroa.cluster.index.port"))
                        .put("discovery.zen.ping.multicast.enabled", false)
                        .put("discovery.zen.fd.ping_timeout", "3s")
                        .put("discovery.zen.fd.ping_retries", 10)
                        .put("script.engine.groovy.indexed.update", true)
                        .put("node.data", properties.getBoolean("zorroa.cluster.index.data", true))
                        .put("node.master", properties.getBoolean("zorroa.cluster.index.master", true))
                        .put("path.plugins", "{path.home}/es-plugins");


        builder.put("index.refresh_interval", "1s");
        builder.put("index.translog.disable_flush", true);
        builder.put("node.local", true);

        Node node = new ZorroaNode(builder.build(), ImmutableSet.of(ArchivistDateScriptPlugin.class));
        node.start();
        return node.client();
    }
}
