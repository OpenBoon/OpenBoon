package com.zorroa.analyst.config;

import com.google.common.collect.ImmutableMap;
import com.zorroa.analyst.Application;
import com.zorroa.sdk.client.archivist.ArchivistClient;import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.filesystem.AbstractFileSystem;
import com.zorroa.sdk.filesystem.ObjectFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by chambers on 2/12/16.
 */
@Configuration
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Autowired
    ApplicationProperties properties;

    @Bean
    public ArchivistClient archivist() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        InputStream trustStoreInput = new ClassPathResource("truststore.p12").getInputStream();
        trustStore.load(trustStoreInput, "zorroa".toCharArray());

        ArchivistClient client =  new ArchivistClient(trustStore, properties.getString("analyst.master.host"));
        return client;
    }

    @Bean
    public InfoEndpoint infoEndpoint() {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("description", "Zorroa Analyst Server");
        map.put("project", "zorroa-analyst");
        Properties props = new Properties();
        try {
            props.load(new ClassPathResource("META-INF/maven/com.zorroa/analyst/pom.properties").getInputStream());
            map.put("version", props.getProperty("version"));
        } catch (Exception e) {
            map.put("version", "test");
            if (!Application.isUnitTest()) {
                logger.warn("Failed to load version info,", e);
            }
        }
        return new InfoEndpoint(ImmutableMap.of("build", map));
    }

    @Bean
    public AsyncTaskExecutor ingestThreadPool() {
        int threads = properties.getInt("analyst.executor.threads");
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }
        else if (threads < 0) {
            threads = Runtime.getRuntime().availableProcessors() / 2;
        }
        System.setProperty("analyst.executor.threads", String.valueOf(threads));

        logger.info("Starting analyst with {} analyze threads", threads);
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("Ingest");
        executor.setQueueCapacity(100);
        return executor;
    }

    @Bean(initMethod="init")
    public ObjectFileSystem fileSystem() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        Class fileSystemClass = classLoader.loadClass(properties.getString("analyst.filesystem.class"));
        AbstractFileSystem fs = (AbstractFileSystem) fileSystemClass.getConstructor(Properties.class).newInstance(
                properties.getProperties("analyst.filesystem.", false));
        return fs;
    }
}
