package com.zorroa.analyst.config;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.zorroa.common.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    public ThreadPoolExecutor analyzeThreadPool() {
        int threads = properties.getInt("analyst.executor.threads");
        if (threads == 0) {
            threads = Runtime.getRuntime().availableProcessors();
        }
        else if (threads < 0) {
            threads = Runtime.getRuntime().availableProcessors() / 2;
        }

        System.setProperty("analyst.executor.threads", String.valueOf(threads));
        BlockingQueue<Runnable> linkedBlockingDeque = new LinkedBlockingDeque<>();

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

    @Bean
    public InfoEndpoint infoEndpoint() throws IOException {

        InfoContributor info = builder -> {
            builder.withDetail("description", "Zorroa Analyst Server");
            builder.withDetail("project", "zorroa-analyst");

            try {
                Properties props = new Properties();
                props.load(new ClassPathResource("version.properties").getInputStream());
                builder.withDetail("version", props.getProperty("build.version"));
                builder.withDetail("date", props.getProperty("build.date"));
                builder.withDetail("user", props.getProperty("build.user"));
                builder.withDetail("commit", props.getProperty("build.id"));
                builder.withDetail("branch", props.getProperty("build.branch"));
                builder.withDetail("dirty", props.getProperty("build.dirty"));
            } catch (IOException e) {
                logger.warn("Can't find info version properties");
            }
        };

        return new InfoEndpoint(ImmutableList.of(info));
    }

}
