package com.zorroa.archivist;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

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

        org.elasticsearch.common.settings.ImmutableSettings.Builder builder =
                ImmutableSettings.settingsBuilder()
                .put("cluster.name", "zorroa")
                .put("node.name", nodeName)
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("cluster.routing.allocation.disk.threshold_enabled", false);

        if (unittest) {
            builder.put("path.data", "unittest/data");
        }

        Node node = nodeBuilder()
                .data(true)
                .local(true)
                .settings(builder.build())
                .node();

        return node.client();
    }

    public String getName() {
        return nodeName;
    }

    public String getHostName() {
        return hostName;
    }

    @Bean
    public RequestMappingHandlerAdapter requestMappingHandlerAdapter() {
        RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
        adapter.setMessageConverters(Lists.newArrayList(
                new MappingJackson2HttpMessageConverter()
        ));
        return adapter;
    }

    @Bean(name="ingestTaskExecutor")
    public TaskExecutor ingestTaskExecutor() {

        if (unittest) {
            SyncTaskExecutor executor = new SyncTaskExecutor();
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
    public TaskExecutor processorTaskExecutor() {

        if (unittest) {
            SyncTaskExecutor executor = new SyncTaskExecutor();
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
