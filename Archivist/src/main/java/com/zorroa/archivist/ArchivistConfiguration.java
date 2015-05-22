package com.zorroa.archivist;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.ByteStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.google.common.base.Preconditions;

@Configuration
public class ArchivistConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    private String nodeName;
    private String hostName;

    public static boolean unittest = false;

    @PostConstruct
    public void init() throws UnknownHostException {
        Random rand = new Random(System.nanoTime());
        int number = rand.nextInt(99999);

        hostName = InetAddress.getLocalHost().getHostName();
        nodeName = String.format("%s_%05d", hostName, number);
    }

    @Bean
    public Client elastic() throws IOException {

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", "zorroa")
                .put("node.name", nodeName)
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("cluster.routing.allocation.disk.threshold_enabled", false)
                .build();

        Node node = nodeBuilder()
                .data(true)
                .local(true)
                .settings(settings)
                .node();

        return node.client();
    }

    public String getName() {
        return nodeName;
    }

    public String getHostName() {
        return hostName;
    }

    @Bean(name="ingestTaskExecutor")
    public AsyncTaskExecutor ingestTaskExecutor() {

        if (unittest) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
            return executor;
        }
        else {
            ThreadPoolTaskExecutor executor =  new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setDaemon(true);
            executor.setMaxPoolSize(4);
            executor.setThreadNamePrefix("ingest");
            return executor;
        }
    }

    @Bean(name="processorTaskExecutor")
    public AsyncTaskExecutor processorTaskExecutor() {

        if (unittest) {
            SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
            return executor;
        }
        else {
            ThreadPoolTaskExecutor executor =  new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setDaemon(true);
            executor.setMaxPoolSize(8);
            executor.setThreadNamePrefix("processor");
            return executor;
        }
    }
}
