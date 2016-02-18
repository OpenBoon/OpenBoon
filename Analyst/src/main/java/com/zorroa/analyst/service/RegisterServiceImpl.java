package com.zorroa.analyst.service;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.archivist.sdk.client.archivist.ArchivistClient;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import com.zorroa.archivist.sdk.domain.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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

    @PostConstruct
    public void init() throws UnknownHostException {
        String protocol = properties.getBoolean("server.ssl.enabled") ? "https" : "http";
        url = protocol + "://" + InetAddress.getLocalHost().getHostAddress() + ":" + properties.getInt("server.port");
        startAsync();
    }

    @Override
    protected void runOneIteration() throws Exception {
        try {
            archivistClient.registerAnalyst(getPing());
        } catch (Exception e) {
            logger.warn("Failed to register as worker node, ", e);
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

    private AnalystPing getPing() throws UnknownHostException {

        ThreadPoolTaskExecutor e = (ThreadPoolTaskExecutor) ingestThreadPool;
        AnalystPing ping = new AnalystPing();
        ping.setUrl(url);
        ping.setQueueSize(e.getThreadPoolExecutor().getQueue().size());
        ping.setThreadsActive(e.getActiveCount());
        ping.setThreadsTotal(e.getPoolSize());
        ping.setData(properties.getBoolean("analyst.index.data"));
        return ping;
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(5, 30, TimeUnit.SECONDS);
    }
}
