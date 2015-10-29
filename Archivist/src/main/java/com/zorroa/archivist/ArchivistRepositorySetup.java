package com.zorroa.archivist;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestProcessorFactory;
import com.zorroa.archivist.domain.IngestState;
import com.zorroa.archivist.processors.AssetMetadataProcessor;
import com.zorroa.archivist.processors.ProxyProcessor;
import com.zorroa.archivist.service.ImageService;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.IngestService;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequestBuilder;
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
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ArchivistRepositorySetup implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    ArchivistConfiguration config;

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    ImageService imageService;

    @Autowired
    Client client;

    @Autowired
    AuthenticationManager authenticationManager;

    @Value("${archivist.index.alias}")
    private String alias;

    @Value("${archivist.snapshot.basePath}")
    private String snapshotPath;

    @Value("${archivist.snapshot.repoName}")
    private String snapshotRepoName;

    private String indexName;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        /*
         * Authorize the startup thread as an admin.
         */
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("admin", "admin")));
        try {
            setupElasticSearchMapping();
            createDefaultIngestPipeline();
            /**
             * TODO: get the snapshot repository working with elastic 1.7
             *
             * createSnapshotRepository();
             */

            restartRunningIngests();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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

    private void createDefaultIngestPipeline() {
        if (ingestService.getIngestPipelines().size() == 0) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("standard");
            builder.addToProcessors(new IngestProcessorFactory(AssetMetadataProcessor.class));
            builder.addToProcessors(new IngestProcessorFactory(ProxyProcessor.class));

            logger.info("Creating 'standard' ingest pipeline");
            ingestService.createIngestPipeline(builder);
        }
    }

    private void createSnapshotRepository() {
        logger.info("Creating snapshot repository \"" + snapshotRepoName + "\" in \"" + snapshotPath + "\"");
        // Later we can add support for S3 buckets or HDFS backups
        Settings settings = ImmutableSettings.builder()
                .put("location", snapshotPath)
                .put("compress", true)
                .put("max_snapshot_bytes_per_sec", "50mb")
                .put("max_restore_bytes_per_sec", "50mb")
                .build();
        PutRepositoryRequestBuilder builder =
                new PutRepositoryRequestBuilder(client.admin().cluster());
        builder.setName(snapshotRepoName)
                .setType("fs")
                .setSettings(settings)
                .execute().actionGet();
    }

    private void restartRunningIngests() {
        // Start running ingests first so they get the active threads
        for (Ingest ingest : ingestService.getAllIngests()) {
            if (ingest.getState() == IngestState.Running) {
                ingestService.setIngestPaused(ingest);      // Set paused to avoid resume error checks
                ingestExecutorService.resume(ingest);
                logger.info("Restarting ingest " + ingest.getId());
            }
        }

        // Start queued ingests after all running ingests
        for (Ingest ingest : ingestService.getAllIngests()) {
            if (ingest.getState() == IngestState.Queued) {
                ingestExecutorService.executeIngest(ingest);
                logger.info("Re-executing queued ingest " + ingest.getId());
            }
        }
    }
}
