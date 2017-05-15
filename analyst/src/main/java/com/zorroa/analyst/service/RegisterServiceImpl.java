package com.zorroa.analyst.service;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private List<Float> load = Lists.newArrayList();

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

    private final AtomicBoolean connected = new AtomicBoolean(false);

    @Override
    protected void runOneIteration() {
        if (Application.isUnitTest()) {
            return;
        }

        try {
            List<String> urls = properties.getList("analyst.master.host");
            for (String url: urls) {
                try {
                    register(url);
                    if (connected.compareAndSet(false, true)) {
                        logger.info("Registered with {}", url);
                    }
                } catch (Exception e) {
                    if (connected.compareAndSet(false, true)) {
                        logger.warn("Failed to register with archivist: {}, {}", url, e.getMessage());
                    }
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

        MasterServerClient client = new MasterServerClient(url);
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadPoolExecutor e = (ThreadPoolExecutor) analyzeThreadPool;

        Map<String, Object> mdata = metrics.invoke();
        Map<String, Object> fixedMdata = Maps.newHashMapWithExpectedSize(mdata.size());
        metrics.invoke().forEach((k,v)-> fixedMdata.put(k.replace('.', '_'), v));

        AnalystT builder = new AnalystT();
        builder.setOs(String.format("%s-%s", osBean.getName(), osBean.getVersion()));
        builder.setThreadCount(properties.getInt("analyst.executor.threads"));
        builder.setArch(osBean.getArch());
        builder.setData(properties.getBoolean("analyst.index.data"));
        builder.setUrl(networkEnvironment.getUri().toString());
        builder.setUpdatedTime(System.currentTimeMillis());
        builder.setQueueSize(e.getQueue().size());
        builder.setThreadsUsed(e.getActiveCount());
        builder.setTaskIds(ImmutableList.of());
        builder.setId(id);
        builder.setTaskIds(processManagerNgService.getTaskIds());
        builder.setLoadAvg(osBean.getSystemLoadAverage());
        builder.setMetrics(Json.serialize(fixedMdata));

        try {
            client.ping(builder);
        } finally {
            client.close();
        }
        return id;
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 15, TimeUnit.SECONDS);
    }
}
