package com.zorroa.archivist;

import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.sdk.processor.SharedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class ArchivistRepositorySetup implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    PluginService pluginService;

    @Autowired
    SharedData sharedData;

    @Value("${zorroa.cluster.index.alias}")
    private String alias;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        /**
         * During unit tests, setupDataSources() is called after the indexes are prepared
         * for a unit test.
         */
        if (!ArchivistConfiguration.Companion.getUnittest()) {
            /**
             * Have async threads inherit the current authorization.
             */
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

            try {
                setupDataSources();
            } catch (IOException e) {
                logger.error("Failed to setup datasources, ", e);
                throw new RuntimeException(e);
            }

            pluginService.installAndRegisterAllPlugins();
            pluginService.installBundledPipelines();
        }
    }

    public void setupDataSources() throws IOException {
        logger.info("Setting up data sources");
        //ElasticClientUtils.createIndexedScripts(client);
        //ElasticClientUtils.createEventLogTemplate(client);
        createSharedPaths();
        refreshIndex();
    }


    public void createSharedPaths() {
        sharedData.create();
    }

    public void refreshIndex() {
        logger.info("Refreshing Elastic Indexes");
        //ElasticClientUtils.refreshIndex(client);
    }
}
