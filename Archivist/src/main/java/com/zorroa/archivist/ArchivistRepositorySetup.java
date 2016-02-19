package com.zorroa.archivist;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.sdk.domain.Ingest;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestState;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.security.InternalAuthentication;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.IngestService;
import com.zorroa.archivist.service.MigrationService;
import org.elasticsearch.action.admin.cluster.repositories.put.PutRepositoryRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.ByteStreams;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ArchivistRepositorySetup implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    IngestService ingestService;

    @Autowired
    IngestExecutorService ingestExecutorService;

    @Autowired
    Client client;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    MigrationService migrationService;

    @Value("${zorroa.common.index.alias}")
    private String alias;

    @Value("${archivist.snapshot.basePath}")
    private String snapshotPath;

    @Value("${archivist.snapshot.repoName}")
    private String snapshotRepoName;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        /**
         * During unit tests, setupDataSources() is called after the indexes are prepared
         * for a unit test.
         */
        if (!ArchivistConfiguration.unittest) {
            /*
             * Authorize the startup thread as an admin.
             */
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
            SecurityContextHolder.getContext().setAuthentication(
                    authenticationManager.authenticate(new InternalAuthentication()));

            migrationService.processAll();

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
    }

    public void setupDataSources() throws IOException {
        logger.info("Setting up data sources");
        createIndexedScripts();
        createEventLogTemplate();
        createDefaultIngestPipeline();
        refreshIndex();
    }

    public void refreshIndex() {
        logger.info("Refreshing index: '{}'", alias);
        client.admin().indices().prepareRefresh(alias).get();
    }

    public void createEventLogTemplate() throws IOException {
        logger.info("Creating event log template");
        ClassPathResource resource = new ClassPathResource("eventlog-template.json");
        byte[] source = ByteStreams.toByteArray(resource.getInputStream());
        client.admin().indices().preparePutTemplate("eventlog").setSource(source).get();
    }

    private void createDefaultIngestPipeline() {
        if (!ingestService.ingestPipelineExists("standard")) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("standard");
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.analyst.ingestors.ImageIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.analyst.ingestors.VideoIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.analyst.ingestors.PdfIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.analyst.ingestors.ProxyProcessor"));
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
