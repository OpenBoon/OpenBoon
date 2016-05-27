package com.zorroa.analyst.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.sdk.client.archivist.ArchivistClient;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ArgumentScanner;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import com.zorroa.sdk.processor.ingest.IngestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
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
    PluginService pluginService;

    @Autowired
    ArchivistClient archivistClient;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    Executor ingestThreadPool;

    private String url;

    private AtomicBoolean registered = new AtomicBoolean(false);

    @PostConstruct
    public void init() throws IOException {
        String protocol = properties.getBoolean("server.ssl.enabled") ? "https" : "http";
        String addr =  getIpAddress();
        url = protocol + "://" + addr + ":" + properties.getInt("server.port");
        System.setProperty("server.address", addr);
        System.setProperty("server.url", url);
        logger.info("External {} interface: {}", protocol, url);
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

    public AnalystPing getPing()  {

        ThreadPoolTaskExecutor e = (ThreadPoolTaskExecutor) ingestThreadPool;
        AnalystPing ping = new AnalystPing();
        ping.setUrl(url);
        ping.setQueueSize(e.getThreadPoolExecutor().getQueue().size());
        ping.setThreadsActive(e.getActiveCount());
        ping.setThreadsTotal(e.getPoolSize());
        ping.setData(properties.getBoolean("analyst.index.data"));

        /**
         * We only need to do this on the first ping.
         */
        if (!registered.get()) {
            ping.setPlugins(getPlugins());
        }

        return ping;
    }

    public List<PluginProperties> getPlugins() {
        List<PluginProperties> result = Lists.newArrayList();
        ArgumentScanner scanner = new ArgumentScanner();
        for (Tuple<PluginProperties, Plugin> tuple : pluginService.getLoadedPlugins()) {
            PluginProperties plugin = tuple.getLeft();
            List<ProcessorProperties> processors = Lists.newArrayList();
            for (Class<? extends IngestProcessor> proc : tuple.getRight().getIngestProcessors()) {
                processors.add(new ProcessorProperties(proc.getName(), ProcessorType.Ingest,
                        scanner.scan(proc)));
            }
            plugin.setProcessors(processors);
            result.add(plugin);
        }
        return result;
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
        return Scheduler.newFixedRateSchedule(1, 30, TimeUnit.SECONDS);
    }

}
