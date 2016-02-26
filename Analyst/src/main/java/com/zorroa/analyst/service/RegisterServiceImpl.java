package com.zorroa.analyst.service;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.ClassPath;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.archivist.sdk.client.archivist.ArchivistClient;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Register this process with the archivist.
 */
@Component
public class RegisterServiceImpl extends AbstractScheduledService implements RegisterService {

    private static final Logger logger = LoggerFactory.getLogger(RegisterServiceImpl.class);

    @Autowired
    ArchivistClient archivistClient;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    Executor ingestThreadPool;

    private String url;

    private AtomicBoolean registered = new AtomicBoolean(false);

    @PostConstruct
    public void init() throws UnknownHostException {
        String protocol = properties.getBoolean("server.ssl.enabled") ? "https" : "http";
        url = protocol + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + properties.getInt("server.port");
        startAsync();
    }

    @Override
    protected void runOneIteration() {
        if (Application.isUnitTest()) {
            return;
        }
        try {
            sendPing();
        } catch (Exception e) {
            logger.warn("Failed to register as worker node, ", e);
            /*
             * If the analyst hasn't registered at least once, then increase
             * the rate at which we try to register.
             */
            if (!registered.get()) {
                for (;;) {
                    try {
                        Thread.sleep(1000);
                        sendPing();
                        return;
                    } catch (InterruptedException ignore) {
                        return;
                    }
                    catch (Exception ex) {
                        logger.warn("Failed to register as worker node, ", ex);
                    }
                }
            }
        }
    }

    @Override
    protected void shutDown() throws Exception {
        try {
            logger.info("Sending shutdown ping");
            archivistClient.shutdownAnalyst(getPing());
        } catch (Exception e) {
            logger.warn("Failed to un-register as worker node, ", e);
        }
    }

    /**
     * Send an analyst ping to the archivist.
     */
    private void sendPing() {
        archivistClient.registerAnalyst(getPing());
        registered.set(true);
    }

    private Set<String> EXCLUDE_INGESTORS =
            ImmutableSet.of("com.zorroa.archivist.sdk.processor.ingest.IngestProcessor");

    private AnalystPing getPing()  {

        List<String> ingestClasses = Lists.newArrayListWithCapacity(30);
        try {
            ClassLoader classLoader = new ProcessorFactory<>().getSiteClassLoader();
            ClassPath classPath = ClassPath.from(classLoader);
            for (ClassPath.ClassInfo info: classPath.getTopLevelClassesRecursive("com.zorroa")) {
                try {
                    Class<?> clazz = classLoader.loadClass(info.getName());
                    if (IngestProcessor.class.isAssignableFrom(clazz) && !EXCLUDE_INGESTORS.contains(info.getName())) {
                        ingestClasses.add(info.getName());
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException ignore) {
                    /*
                     * This fails for dynamically loaded classes that inherit from an
                     * external base class.
                     */
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to load ingestors, ", e);
        }

        ThreadPoolTaskExecutor e = (ThreadPoolTaskExecutor) ingestThreadPool;
        AnalystPing ping = new AnalystPing();
        ping.setUrl(url);
        ping.setQueueSize(e.getThreadPoolExecutor().getQueue().size());
        ping.setThreadsActive(e.getActiveCount());
        ping.setThreadsTotal(e.getPoolSize());
        ping.setData(properties.getBoolean("analyst.index.data"));
        ping.setIngestProcessorClasses(ingestClasses);
        return ping;
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 30, TimeUnit.SECONDS);
    }
}
