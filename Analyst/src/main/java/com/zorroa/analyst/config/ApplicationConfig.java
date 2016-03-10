package com.zorroa.analyst.config;

import com.zorroa.archivist.sdk.client.archivist.ArchivistClient;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.filesystem.AbstractFileSystem;
import com.zorroa.archivist.sdk.filesystem.ObjectFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.InputStream;
import java.security.KeyStore;
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
    public AsyncTaskExecutor ingestThreadPool() {

        int threads = properties.getInt("analyst.executor.threads");
        if (threads <= 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }

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
