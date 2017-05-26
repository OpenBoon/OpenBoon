package com.zorroa.archivist;

import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.repository.StartupDao;
import com.zorroa.archivist.repository.UserDao;
import com.zorroa.archivist.service.MigrationService;
import com.zorroa.archivist.service.PluginService;
import com.zorroa.common.config.NetworkEnvironment;
import com.zorroa.common.elastic.ElasticClientUtils;
import com.zorroa.sdk.processor.SharedData;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class ArchivistRepositorySetup implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ArchivistRepositorySetup.class);

    @Autowired
    PluginService pluginService;

    @Autowired
    Client client;

    @Autowired
    MigrationService migrationService;

    @Autowired
    SharedData sharedData;

    @Autowired
    StartupDao startupDao;

    @Autowired
    NetworkEnvironment networkEnvironment;

    @Autowired
    JavaMailSender mailSender;

    @Autowired
    UserDao userDao;

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
        }

        /**
         * Register plugins.
         */
        pluginService.installAndRegisterAllPlugins();

        // We register these manually in unittests
        if (!ArchivistConfiguration.unittest) {
            pluginService.installBundledPipelines();
        }

        logStartup();
    }

    public void logStartup() {
        int number = startupDao.create();
        logger.info("Server startup #{}", number);

        /**
         * On the first startup which is not on-prem (enterprise) reset
         * the admin credentials and send an email to zorroa.
         */
        if (number == 1 && !networkEnvironment.getLocation().equals("on-prem")) {
            String pass = HttpUtils.randomString(12);
            userDao.generateHmacKey("admin");
            userDao.setPassword(userDao.get("admin"), pass);
            sendNewServerEmail(pass);
        }
    }

    public void sendNewServerEmail(String pass) {
        StringBuilder text = new StringBuilder(1024);
        text.append("Host: " + networkEnvironment.getPublicUri() + "\n");
        text.append("Region: " + networkEnvironment.getLocation() + "\n");
        text.append("Admin: " + pass + "\n");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("Zorroa Server Bot <noreply@zorroa.com>");
        message.setReplyTo("Zorroa Server Bot <noreply@zorroa.com>");
        message.setTo("support@zorroa.com");
        message.setSubject("New Archivist '" + networkEnvironment.getPublicUri() + "' is online");
        message.setText(text.toString());

        try {
            mailSender.send(message);
        } catch (Exception e) {
            logger.warn("Failed to send initial startup email, pass is '{}' {}", pass, e);
        }
    }

    public void setupDataSources() throws IOException {
        logger.info("Setting up data sources");
        ElasticClientUtils.createIndexedScripts(client);
        ElasticClientUtils.createEventLogTemplate(client);
        createSharedPaths();
        refreshIndex();
    }


    public void createSharedPaths() {
        sharedData.create();
    }

    public void refreshIndex() {
        logger.info("Refreshing Elastic Indexes");
        ElasticClientUtils.refreshIndex(client);
    }
}
