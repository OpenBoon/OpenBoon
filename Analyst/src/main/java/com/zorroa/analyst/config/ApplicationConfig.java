package com.zorroa.analyst.config;

import com.zorroa.archivist.sdk.client.archivist.ArchivistClient;
import com.zorroa.common.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Bean
    public ApplicationProperties properties() {
        return new ApplicationProperties();
    }

    @Bean
    public ArchivistClient archivist() throws Exception {

        ArchivistClient client =  new ArchivistClient(properties().getString("analyst.master.host"));

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        InputStream keystoreInput = new ClassPathResource("keystore.p12").getInputStream();
        keystore.load(keystoreInput, "zorroa".toCharArray());

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        InputStream trustStoreInput = new ClassPathResource("truststore.p12").getInputStream();
        trustStore.load(trustStoreInput, "zorroa".toCharArray());

        client.init(keystore, "zorroa", trustStore);
        return client;
    }

    @Bean
    public Executor ingestThreadPool() {

        int threads = Math.max(properties().getInt("analyst.executor.threads"),
                Runtime.getRuntime().availableProcessors());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setThreadNamePrefix("Ingest");
        executor.setQueueCapacity(1000);
        return executor;
    }
}
