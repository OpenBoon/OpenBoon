package com.zorroa.analyst.service;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.analyst.ArchivistClient;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.domain.AnalystSpec;
import com.zorroa.common.domain.AnalystState;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    ProcessManagerService processManagerService;

    @Autowired
    ArchivistClient archivistClient;

    private String url;
    private String id;
    private List<Float> load = Lists.newArrayList();

    private boolean registered = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent)  {
        determineUniqueId();
        determineExternalHttpInterface();
        startAsync();
    }

    private void determineExternalHttpInterface() {
        String protocol = properties.getBoolean("server.ssl.enabled") ? "https" : "http";
        String addr = "127.0.0.1";

        try {
            addr = getIpAddress();
        } catch (IOException e) {
            logger.warn("Failure determining local IP address", e);
        }

        String existingUrl = properties.getString("server.url", null);
        if (existingUrl != null) {
            url = existingUrl;
        }
        else {
            url = protocol + "://" + addr + ":" + properties.getInt("server.port");
        }

        System.setProperty("server.url", url);
        System.setProperty("server.address", addr);

        logger.info("External {} interface: {}", protocol, url);
    }

    public void determineUniqueId() {
        File keyFile =new File("analyst.key");
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
            register();
        } catch (Exception e) {
            logger.warn("Failed to register as worker node, ", e);
            /*
             * If the analyst hasn't registered at least once, then increase
             * the rate at which we try to register.
             */
            if (!registered) {
                for (;;) {
                    try {
                        Thread.sleep(1000);
                        register();
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
        // TODO: set the state of the analyst to shutdown
    }

    public String register() {

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadPoolExecutor e = (ThreadPoolExecutor) analyzeThreadPool;

        Map<String, Object> mdata = metrics.invoke();
        Map<String, Object> fixedMdata = Maps.newHashMapWithExpectedSize(mdata.size());
        metrics.invoke().forEach((k,v)-> fixedMdata.put(k.replace('.', '_'), v));

        AnalystSpec builder = new AnalystSpec();
        builder.setState(AnalystState.UP);
        builder.setStartedTime(System.currentTimeMillis());
        builder.setOs(String.format("%s-%s", osBean.getName(), osBean.getVersion()));
        builder.setThreadCount(properties.getInt("analyst.executor.threads"));
        builder.setArch(osBean.getArch());
        builder.setData(properties.getBoolean("analyst.index.data"));
        builder.setUrl(url);
        builder.setUpdatedTime(System.currentTimeMillis());
        builder.setState(AnalystState.UP);
        builder.setLoad(load);
        builder.setQueueSize(e.getQueue().size());
        builder.setThreadsUsed(e.getActiveCount());
        builder.setMetrics(fixedMdata);
        builder.setTaskIds(ImmutableList.of());
        builder.setRemainingCapacity(e.getQueue().remainingCapacity());
        builder.setId(id);
        builder.setTaskIds(processManagerService.getTaskIds());
        archivistClient.register(builder);
        return id;
    }

    public String getIpAddress() throws IOException {

        String ip = properties.getString("server.address", null);
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }

        Set<String> allowedDevices = Sets.newHashSet();
        String deviceString = properties.getString("server.devices", null);
        if (deviceString != null) {
            allowedDevices.addAll(Splitter.on(",").trimResults().splitToList(deviceString));
        }

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            if (!ni.isUp()) {
                continue;
            }
            if (!allowedDevices.isEmpty()) {
                if (!allowedDevices.contains(ni.getName())) {
                    continue;
                }
            }
            Enumeration<InetAddress> nias = ni.getInetAddresses();
            while (nias.hasMoreElements()) {
                InetAddress ia = nias.nextElement();
                if (!ia.isLinkLocalAddress()
                        && !ia.isLoopbackAddress()
                        && ia instanceof Inet4Address) {
                    return ia.getHostAddress().toString();
                }
            }
        }

        return InetAddress.getLocalHost().getHostAddress();
    }


    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(1, 15, TimeUnit.SECONDS);
    }
}
