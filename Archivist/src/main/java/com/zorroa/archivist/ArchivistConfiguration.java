package com.zorroa.archivist;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.ByteStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.indices.IndexMissingException;
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

@Configuration
public class ArchivistConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    @Value("${archivist.index.alias}")
    private String alias;

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

        Client client = node.client();
        setupElasticSearchMapping(client);

        return client;
    }

    @PreDestroy
    void shutdown() throws ElasticsearchException, IOException {
        elastic().close();
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

    /**
     * Automatically sets up elastic search if its not setup already.
     *
     * @param client
     * @throws IOException
     */
    private void setupElasticSearchMapping(Client client) throws IOException {

        ClassPathResource resource = new ClassPathResource("elastic-mapping.json");
        byte[] mappingSource = ByteStreams.toByteArray(resource.getInputStream());

        /*
         * Eventually we'll have to keep track of this number somewhere for
         * re-mapping purposes, but for now we're hard coding to 1
         */
        String indexName = String.format("%s_%02d", alias, 1);
        logger.info("Setting up ElasticSearch index: {} with alias: {}", indexName, alias);


        client.admin()
            .indices()
            .prepareCreate(indexName)
            .setSource(mappingSource)
            .addAlias(new Alias(alias))
            .execute(new ActionListener<CreateIndexResponse>() {
                @Override
                public void onResponse(CreateIndexResponse response) {
                    //
                }

                @Override
                public void onFailure(Throwable e) {
                    if (e instanceof IndexAlreadyExistsException) {
                        return;
                    }
                    logger.warn("ElasticSearch init warning on {}", indexName, e);
                    throw new RuntimeException("Faled to setup elastic search: "+ e, e);
                }
            });
    }
}
