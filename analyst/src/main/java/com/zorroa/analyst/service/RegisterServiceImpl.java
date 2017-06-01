package com.zorroa.analyst.service;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.common.cluster.client.MasterServerClient;
import com.zorroa.common.cluster.thrift.AnalystT;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.config.NetworkEnvironment;
import com.zorroa.sdk.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Register this process with the archivist.
 */
@Component
public class RegisterServiceImpl extends AbstractScheduledService implements RegisterService, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(RegisterServiceImpl.class);

    @Autowired
    ApplicationProperties properties;

    @Autowired
    MetricsEndpoint metrics;

    @Autowired
    Executor analyzeThreadPool;

    @Autowired
    ProcessManagerNgService processManagerNgService;

    @Autowired
    NetworkEnvironment networkEnvironment;

    private String id;
    private ExecutorService registerPool;
    private BlockingQueue<Runnable> queue;
    private final Map<String, ConnectionState> connected = Maps.newConcurrentMap();

    private enum ConnectionState {
        SUCCESS,
        FAIL
    }

    public RegisterServiceImpl() {
        /**
         * Setup a bounded threadpool for pinging archivists
         */
        queue = new LinkedBlockingDeque<>(250);
        registerPool = new ThreadPoolExecutor(4, 4, 300,
            TimeUnit.SECONDS, queue, new ThreadPoolExecutor.AbortPolicy());

    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent)  {
        determineUniqueId();
        startAsync();
    }

    public void determineUniqueId() {
        File keyFile = new File(properties.getString("analyst.key.file"));
        if (!keyFile.exists()) {
            try {
                Files.write(UUID.randomUUID().toString().replace("-", "").getBytes(), keyFile);
            } catch (Exception e) {
                throw new RuntimeException("Unable to write unique key to file: " + keyFile.getName(), e);
            }
        }

        try {
            id = Files.readFirstLine(keyFile, Charsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Unable to read unique key from file: " + keyFile.getName(), e);
        }

        if (id == null) {
            throw new RuntimeException("Unable to determine analyst ID");
        }

        if (!StringUtils.isAlphanumeric(id) || id.length() > 32) {
            throw new RuntimeException("Invalid analyst id: '" +
                    id + "', must be alpha numeric string of 32 chars or less.");
        }
        System.setProperty("analyst.id", id);
        logger.info("Analyst ID: {}", id);
    }

    @Override
    protected void runOneIteration() {
        if (Application.isUnitTest()) {
            return;
        }

        try {
            List<String> urls = properties.getList("analyst.master.host");
            for (String url: urls) {
                try {
                    registerPool.execute(() -> register(url));
                } catch (Exception e) {
                    logger.warn("Unable to queue archivist register command, queue is full, {}", e.getMessage());
                }
            }
        }
        catch (Exception e) {
            logger.warn("Unable to determine archivist master list: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void shutDown() throws Exception {
        // TODO: set the state of the analyst to shutdown
    }

    public String register(String url) {

        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            ThreadPoolExecutor e = (ThreadPoolExecutor) analyzeThreadPool;

            Map<String, Object> mdata = metrics.invoke();
            Map<String, Object> fixedMdata = Maps.newHashMapWithExpectedSize(mdata.size());
            metrics.invoke().forEach((k, v) -> fixedMdata.put(k.replace('.', '_'), v));

            AnalystT builder = new AnalystT();
            builder.setOs(String.format("%s-%s", osBean.getName(), osBean.getVersion()));
            builder.setThreadCount(properties.getInt("analyst.executor.threads"));
            builder.setArch(osBean.getArch());
            builder.setData(properties.getBoolean("analyst.index.data"));
            builder.setUrl(networkEnvironment.getClusterAddr());
            builder.setUpdatedTime(System.currentTimeMillis());
            builder.setQueueSize(e.getQueue().size());
            builder.setThreadsUsed(e.getActiveCount());
            builder.setTaskIds(ImmutableList.of());
            builder.setId(id);
            builder.setTaskIds(processManagerNgService.getTaskIds());
            builder.setLoadAvg(osBean.getSystemLoadAverage());
            builder.setMetrics(Json.serialize(fixedMdata));

            ConnectionState state = connected.get(url);
            MasterServerClient client = new MasterServerClient(url);
            try {
                client.setMaxRetries(0);
                client.setConnectTimeout(2000);
                client.setSocketTimeout(2000);
                client.ping(builder);
                if (!ConnectionState.SUCCESS.equals(state)) {
                    logger.info("Registered with {}", url);
                    connected.put(url, ConnectionState.SUCCESS);
                }

            } catch (Exception ex) {
                if (!ConnectionState.FAIL.equals(state)) {
                    logger.warn("No connection to archivist {}", url);
                    connected.put(url, ConnectionState.FAIL);
                }
            } finally {
                client.close();
            }
        } catch (Exception e) {
            logger.warn("Failed to register with {}", url, e);
        }
        return id;
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 15, TimeUnit.SECONDS);
    }
}

