package com.zorroa.archivist;

import com.zorroa.archivist.service.MigrationService;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.common.domain.EventSpec;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.common.repository.ClusterSettingsDao;
import com.zorroa.common.repository.EventLogDao;
import com.zorroa.sdk.config.ApplicationProperties;
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

import java.io.File;
import java.io.IOException;


@Component
public class ArchivistRepositorySetup implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    PluginService pluginService;

    @Autowired
    Client client;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    MigrationService migrationService;

    @Autowired
    EventLogDao eventLogDao;

    @Autowired
    ClusterSettingsDao clusterSettingsDao;

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
            /**
             * Have async threads inherit the current authorization.
             */
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

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
            eventLogDao.info(EventSpec.log("Archivist started"));
        }
    }

    public void setupDataSources() throws IOException {
        logger.info("Setting up data sources");
        ElasticClientUtils.createIndexedScripts(client);
        ElasticClientUtils.createEventLogTemplate(client);
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
        clusterSettingsDao.save(properties);
    }
}
