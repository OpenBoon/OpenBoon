package com.zorroa.analyst.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.zorroa.analyst.Application;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.config.ApplicationProperties;
import com.zorroa.sdk.domain.AnalystBuilder;
import com.zorroa.sdk.domain.AnalystState;
import com.zorroa.sdk.domain.AnalystUpdateBuilder;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.plugins.Plugin;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ArgumentScanner;
import com.zorroa.sdk.processor.IngestProcessor;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    PluginService pluginService;

    @Autowired
    AnalystDao analystDao;

    @Autowired
    ApplicationProperties properties;

    @Autowired
    MetricsEndpoint metrics;

    @Autowired
    Executor analyzeThreadPool;

    private String url;
    private String id;
    private List<Float> load = Lists.newArrayList();

    private boolean registered = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent)  {
        String protocol = properties.getBoolean("server.ssl.enabled") ? "https" : "http";
        String addr = "127.0.0.1";

        try {
            addr = getIpAddress();
        } catch (IOException e) {
            logger.warn("Failure determining local IP address", e);
        }
        url = protocol + "://" + addr + ":" + properties.getInt("server.port");
        System.setProperty("server.address", addr);

        String existingUrl = properties.getString("server.url", null);
        if (existingUrl == null) {
            System.setProperty("server.url", url);
        }
        logger.info("External {} interface: {}", protocol, url);
        startAsync();
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

        if (!registered) {
            AnalystBuilder builder = new AnalystBuilder();
            builder.setState(AnalystState.UP);
            builder.setStartedTime(System.currentTimeMillis());
            builder.setOs(String.format("%s-%s", osBean.getName(), osBean.getVersion()));
            builder.setThreadCount(properties.getInt("analyst.executor.threads"));
            builder.setArch(osBean.getArch());
            builder.setData(properties.getBoolean("analyst.index.data"));
            builder.setUrl(url);
            builder.setPlugins(getPlugins());
            builder.setUpdatedTime(System.currentTimeMillis());
            builder.setState(AnalystState.UP);
            builder.setLoad(load);
            builder.setQueueSize(e.getQueue().size());
            builder.setThreadsUsed(e.getActiveCount());
            builder.setMetrics(fixedMdata);
            id = analystDao.register(builder);
            registered = true;
        }
        else if (id != null) {
            AnalystUpdateBuilder update = new AnalystUpdateBuilder();
            update.setUpdatedTime(System.currentTimeMillis());
            update.setState(AnalystState.UP);
            update.setLoad(load);
            update.setQueueSize(e.getQueue().size());
            update.setThreadsUsed(e.getActiveCount());
            update.setMetrics(fixedMdata);
            analystDao.update(id, update);
        }

        return id;
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
        return Scheduler.newFixedRateSchedule(1, 15, TimeUnit.SECONDS);
    }
}
