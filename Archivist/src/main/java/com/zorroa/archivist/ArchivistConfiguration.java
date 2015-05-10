package com.zorroa.archivist;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.net.InetAddress;

import javax.annotation.PreDestroy;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ArchivistConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistConfiguration.class);

    @Value("${archivist.index.alias}")
    private String alias;

    @Bean
    public Client elastic() throws IOException {

        Settings settings = ImmutableSettings.settingsBuilder()
                .put("cluster.name", "zorroa")
                .put("node.name", InetAddress.getLocalHost().getHostName())
                .put("discovery.zen.ping.multicast.enabled", false)
                .build();

        Node node = nodeBuilder()
                .data(true)
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
            .execute(new ActionListener<CreateIndexResponse>() {
                @Override
                public void onResponse(CreateIndexResponse response) {
                    client.admin()
                    .indices()
                    .prepareAliases()
                    .addAlias(alias, indexName)
                    .execute()
                    .actionGet();
                }

                @Override
                public void onFailure(Throwable e) {
                    logger.info("ElasticSearch index: {} already exists", indexName);
                }
            });
    }
}
