package com.zorroa.archivist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.zorroa.archivist.aggregators.DateAggregator;
import com.zorroa.archivist.aggregators.IngestPathAggregator;
import com.zorroa.archivist.security.InternalAuthentication;
import com.zorroa.archivist.service.IngestExecutorService;
import com.zorroa.archivist.service.IngestService;
import com.zorroa.archivist.service.MigrationService;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.common.service.EventLogService;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.domain.EventLogMessage;
import com.zorroa.sdk.domain.IngestPipelineBuilder;
import com.zorroa.sdk.processor.ProcessorFactory;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.File;
import java.util.Map;


@Component
public class ArchivistRepositorySetup implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    PluginService pluginService;

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

    @Autowired
    EventLogService eventLogService;

    @Autowired
    ApplicationProperties properties;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

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
             * Register plugins.
             */
            pluginService.registerAllPlugins();

            eventLogService.log(new EventLogMessage("Archivist Started")
                    .setType("Archivist Started"));
        }
    }

    public void setupDataSources() throws IOException {
        logger.info("Setting up data sources");
        ElasticClientUtils.createIndexedScripts(client);
        ElasticClientUtils.createEventLogTemplate(client);
        createDefaultIngestPipeline();
        createSharedPaths();
        writeClusterConfiguration();
        refreshIndex();
    }

    public void createSharedPaths() {
        properties.getMap("zorroa.cluster.").forEach((k,v)-> {
            if (k.contains(".path.")) {
                File dir = new File((String)v);
                boolean result = dir.mkdirs();
                if (!result) {
                    try {
                        dir.setLastModified(System.currentTimeMillis());
                    } catch (Exception e) {
                    }
                    if (!dir.exists()) {
                        throw new RuntimeException("Failed to setup shared storage directory '" +
                                dir + "', could not make directory and it does not exist.");
                    }
                }
            }
        });
    }

    public void refreshIndex() {
        logger.info("Refreshing index: '{}'", alias);
        client.admin().indices().prepareRefresh(alias).execute();
    }

    public void writeClusterConfiguration() {
        // Remove the periods.
        Map<String, Object> source = Maps.newHashMap();
        properties.getMap("zorroa.cluster.").forEach((k,v)-> {
            if (k.contains(".path.")) {
                try {
                    v = new File((String)v).getAbsoluteFile().getCanonicalPath();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to resolve relative external config path: " + v);
                }
            }
            source.put(k.replace('.', '_'), v);
        });

        client.prepareIndex(alias, "config", "current")
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(source)
                .get();
    }

    private void createDefaultIngestPipeline() {
        if (!ingestService.ingestPipelineExists("standard")) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("standard");
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.FilePathIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ImageIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.VideoIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.PdfIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ProxyIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.CaffeIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.FaceIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ColorAnalysisIngestor"));
            builder.addToAggregators(new ProcessorFactory<>(DateAggregator.class));
            builder.addToAggregators(new ProcessorFactory<>(IngestPathAggregator.class));
            ingestService.createIngestPipeline(builder);
        }

        if (!ingestService.ingestPipelineExists("production")) {
            IngestPipelineBuilder builder = new IngestPipelineBuilder();
            builder.setName("production");
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.FilePathIngestor",
                    ImmutableMap.of("representations", ImmutableList.of(ImmutableMap.of("primary", "blend")))));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.BlenderIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ImageIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.VideoIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ShotgunIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ProxyIngestor"));
            builder.addToProcessors(new ProcessorFactory<>("com.zorroa.plugins.ingestors.ColorAnalysisIngestor"));
            builder.addToAggregators(new ProcessorFactory<>(DateAggregator.class));
            builder.addToAggregators(new ProcessorFactory<>(IngestPathAggregator.class));
            ingestService.createIngestPipeline(builder);
        }
    }
}
