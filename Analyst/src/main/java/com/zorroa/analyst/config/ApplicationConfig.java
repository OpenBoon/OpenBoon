package com.zorroa.analyst.config;

import com.zorroa.archivist.sdk.client.archivist.ArchivistClient;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.Executor;

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
    public Executor ingestThreadPool() {

        int threads = Math.max(properties.getInt("analyst.executor.threads"),
                Runtime.getRuntime().availableProcessors());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("Ingest");
        executor.setQueueCapacity(1000);
        return executor;
    }

    @Bean(initMethod="init")
    public ObjectFileSystem fileSystem() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        Class fileSystemClass = classLoader.loadClass(properties.getString("analyst.filesystem.class"));
        ObjectFileSystem fs = (ObjectFileSystem) fileSystemClass.newInstance();
        fs.setLocation(properties.getString("analyst.filesystem.location"));
        return fs;
    }
}
