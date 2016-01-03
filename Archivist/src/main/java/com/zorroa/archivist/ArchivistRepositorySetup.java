package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.processors.ImageIngestor;
import com.zorroa.archivist.processors.ProxyProcessor;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestState;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.service.ImageService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.service.IngestExecutorService;
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
import java.util.Map;

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
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
        SecurityContextHolder.getContext().setAuthentication(
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("admin", "admin")));

        try {
            setupDataSources();
        } catch (IOException e) {
            logger.error("Failed to setup datasources, ", e);
            throw new RuntimeException(e);
        }
        /**
         * TODO: get the snapshot repository working with elastic 1.7
         *
         * createSnapshotRepository();
         */
        restartRunningIngests();
    }

    public void setupDataSources() throws IOException {
        logger.info("Setting up data sources");
        setupElasticSearchMapping();
        createIndexedScripts();
        createEventLogTemplate();
        createDefaultIngestPipeline();

        refreshIndex();
    }

    public void setupElasticSearchMapping() throws IOException {

        ClassPathResource resource = new ClassPathResource("elastic-mapping.json");
        byte[] mappingSource = ByteStreams.toByteArray(resource.getInputStream());

        /*
         * Eventually we'll have to keep track of this number somewhere for
         * re-mapping purposes, but for now we're hard coding to 1
         */
        indexName = String.format("%s_%02d", alias, 1);

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

    public void createEventLogTemplate() throws IOException {
        logger.info("creating event log template");
        ClassPathResource resource = new ClassPathResource("eventlog-template.json");
        byte[] source = ByteStreams.toByteArray(resource.getInputStream());
        client.admin().indices().preparePutTemplate("eventlog").setSource(source).get();
    }

    private void createDefaultIngestPipeline() {
        if (ingestService.getIngestPipelines().size() == 0) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("standard");
            builder.addToProcessors(new ProcessorFactory<>(ImageIngestor.class));
            builder.addToProcessors(new ProcessorFactory<>(ProxyProcessor.class));
            logger.info("Creating 'standard' ingest pipeline");
            ingestService.createIngestPipeline(builder);
        }
    }

    public void createIndexedScripts() {
        logger.info("Creating indexed scripts");

        Map<String, Object> script1 = ImmutableMap.of(
            "script", "if (ctx._source.exports == null ) {  ctx._source.exports = [exportId] } " +
                        "else { ctx._source.exports += exportId; ctx._source.exports = ctx._source.exports.unique(); }",
            "params", "exportId");

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("asset_append_export")
                .setSource(script1)
                .get();

        Map<String, Object> script2 = ImmutableMap.of(
                "script", "if (ctx._source.folders == null ) { ctx._source.folders = [folderId] } else " +
                        "{ ctx._source.folders += folderId; ctx._source.folders = ctx._source.folders.unique(); }",
                "params", "folderId");

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("asset_append_folder")
                .setSource(script2)
                .get();

        Map<String, Object> script3 = ImmutableMap.of(
                "script", "if (ctx._source.folders != null ) { ctx._source.folders.removeIf( {f -> f == folderId} )}",
                "params", "folderId");

        client.preparePutIndexedScript()
                .setScriptLang("groovy")
                .setId("asset_remove_folder")
                .setSource(script3)
                .get();
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
