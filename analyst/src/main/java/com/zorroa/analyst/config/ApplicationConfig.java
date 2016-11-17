package com.zorroa.analyst.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.repository.ClusterSettingsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Created by chambers on 2/12/16.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationConfig {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

    @Autowired
    ApplicationProperties properties;

    @Bean
    public ArchivistClient archivist() throws Exception {
        ArchivistClient client = new ArchivistClient(properties.getString("analyst.master.host"));

        if (!Application.isUnitTest()) {
            logger.info("Loading configuration from {}", properties.getString("analyst.master.host"));
            while(true) {
                try {
                    Map<String, String> settings = client.getClusterSettings();
                    settings.forEach((k, v) -> {
                        k = k.replace(ClusterSettingsDao.DELIMITER, ".");
                        System.setProperty(k, v);
                        logger.info("setting property: {}={}", k, v);
                    });
                    break;
                } catch (Exception e) {
                    logger.warn("Waiting for archivist to start....: {}", e.getMessage());
                    Thread.sleep(1000);
                }
            }
        }

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
    public ThreadPoolExecutor analyzeThreadPool() {
        int maxQueueSize = properties.getInt("analyst.executor.maxQueueSize");
        int threads = properties.getInt("analyst.executor.threads");
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }
        else if (threads < 0) {
            threads = Runtime.getRuntime().availableProcessors() / 2;
        }

        System.setProperty("analyst.executor.threads", String.valueOf(threads));
        BlockingQueue<Runnable> linkedBlockingDeque = new LinkedBlockingDeque<>(maxQueueSize);

        ThreadPoolExecutor tp = new ThreadPoolExecutor(threads, threads, 30,
                TimeUnit.MINUTES, linkedBlockingDeque,
                new ThreadPoolExecutor.AbortPolicy());

        return tp;
    }

    @Bean
    @Autowired
    public ListeningExecutorService analyzeExecutor(ThreadPoolExecutor analyzeThreadPool) {
        return MoreExecutors.listeningDecorator(analyzeThreadPool);
    }
}
