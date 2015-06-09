package com.zorroa.archivist;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.ByteStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.domain.StandardRoles;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.processors.AssetMetadataProcessor;
import com.zorroa.archivist.processors.ProxyProcessor;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.service.IngestService;

@Component
public class ArchivistRepositorySetup {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    ArchivistConfiguration config;

    @Autowired
    IngestService ingestService;

    @Autowired
    Client client;

    @Autowired
    UserDao userDao;

    @Value("${archivist.index.alias}")
    private String alias;

    private String indexName;

    @PostConstruct
    public void init() throws IOException {
        setupElasticSearchMapping();
        createDefaultUsers();
    }
     /**
     * Automatically sets up elastic search if its not setup already.
     *
     * @param client
     * @throws IOException
     */
    private void setupElasticSearchMapping() throws IOException {

        ClassPathResource resource = new ClassPathResource("elastic-mapping.json");
        byte[] mappingSource = ByteStreams.toByteArray(resource.getInputStream());

        /*
         * Eventually we'll have to keep track of this number somewhere for
         * re-mapping purposes, but for now we're hard coding to 1
         */
        indexName = String.format("%s_%02d", alias, 1);

        if (ArchivistConfiguration.unittest) {
            logger.info("Unit test, deleting exiting index");
            try {
                client.admin().indices().prepareClose(indexName).get();
                client.admin().indices().prepareDelete(indexName).get();
            } catch (org.elasticsearch.indices.IndexMissingException ignore) {
                // Index might not have been setup yet.
            }
        }

        try {
            logger.info("Setting up ElasticSearch index: {} with alias: {}", indexName, alias);
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("number_of_shards", 1)
                    .put("number_of_replicas", 0)
                    .build();

            client.admin()
                .indices()
                .prepareCreate(indexName)
                .setSettings(settings)
                .setSource(mappingSource)
                .addAlias(new Alias(alias))
                .get();

            /*
             * Add the default configuration docs.
             */
            createDefaultProxyConfiguration();
            createDefaultIngestPipeline();
            /*
             * Once all default docs are made we refresh the index.
             */
            refreshIndex();

        } catch (IndexAlreadyExistsException ignore) {
            logger.info("Index {}/{} was already setup", indexName, alias);

        } catch (Exception e) {
            throw new RuntimeException("Failed to setup elasticsearch, unexpected: " + e, e);
        }
    }

    public void refreshIndex() {
        logger.info("refreshing index: '{}'", alias);
        client.admin().indices().prepareRefresh(alias).get();
    }

    private void createDefaultUsers()  {
        logger.info("Creating standard users");

        UserBuilder adminBuilder = new UserBuilder();
        adminBuilder.setEmail("admin@zorrao.com");
        adminBuilder.setFirstName("Admin");
        adminBuilder.setLastName("Admin");
        adminBuilder.setUsername("admin");
        adminBuilder.setPassword("admin");
        adminBuilder.setRoles(Sets.newHashSet(StandardRoles.ADMIN));
        userDao.create(adminBuilder);

        UserBuilder userBuilder = new UserBuilder();
        userBuilder.setEmail("user@zorrao.com");
        userBuilder.setFirstName("User");
        userBuilder.setLastName("User");
        userBuilder.setUsername("user");
        userBuilder.setPassword("user");
        userBuilder.setRoles(Sets.newHashSet(StandardRoles.USER));
        userDao.create(userBuilder);
    }

    private void createDefaultProxyConfiguration() throws ElasticsearchException, Exception {
        ClassPathResource resource = new ClassPathResource("proxy-config.json");
        Preconditions.checkNotNull(resource, "Unable to find proxy-config.json");
        byte[] source = ByteStreams.toByteArray(resource.getInputStream());

        logger.info("Creating 'standard' proxy configuration");
        client.prepareIndex(alias, "proxy-config", "standard")
            .setSource(source)
            .get();
    }

    private void createDefaultIngestPipeline() throws ElasticsearchException, Exception {
        IngestPipelineBuilder builder = new IngestPipelineBuilder();
        builder.setId("standard");
        builder.addToProcessors(new IngestProcessorFactory(AssetMetadataProcessor.class));
        builder.addToProcessors(new IngestProcessorFactory(ProxyProcessor.class));

        logger.info("Creating 'standard' ingest pipeline");
        ingestService.createIngestPipeline(builder);
    }
}
