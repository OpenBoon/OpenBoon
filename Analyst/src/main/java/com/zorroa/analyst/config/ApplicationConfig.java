package com.zorroa.analyst.config;

import com.zorroa.archivist.sdk.client.archivist.ArchivistClient;
import com.zorroa.common.config.ApplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Created by chambers on 2/12/16.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public ApplicationProperties properties() {
        return new ApplicationProperties();
    }

    @Bean
    public ArchivistClient archivist() {
        return new ArchivistClient(properties().getString("analyst.master.host"));
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
