package com.zorroa.archivist.service;

import com.zorroa.archivist.repository.AnalystDao;
import com.zorroa.archivist.repository.PluginDao;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.domain.AnalystState;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@Service
@Transactional
public class AnalystServiceImpl implements AnalystService {

    @Autowired
    AnalystDao analystDao;

    @Autowired
    PluginDao pluginDao;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void register(AnalystPing ping) {

        if (!analystDao.update(ping)) {
            analystDao.create(ping);
        }
        else {
            /*
             * Reset the host state to up.  Eventually, we'll only want to move the state to up
             * if the node is in a certain state, which is why this isn't part of the
             * standard update.
             */
            analystDao.setState(ping.getUrl(), AnalystState.UP);
        }

        /*
         * A plugin list has been sent. Add it to the DB so we have a record
         * of all installed plugins.
         */
        if (ping.getPlugins() != null) {
            for (PluginProperties plugin: ping.getPlugins()) {
                int id = pluginDao.create(plugin);
                for (ProcessorProperties processor: plugin.getProcessors()) {
                    pluginDao.addProcessor(id, processor);
                }
            }
        }
    }

    @Override
    public void shutdown(AnalystPing ping) {
        /*
         * Do a final update then then set the state to shutdown.
         */
        if (analystDao.update(ping)) {
            analystDao.setState(ping.getUrl(), AnalystState.SHUTDOWN, AnalystState.UP);
        }
    }

    @Override
    public Analyst get(String url) {
        return analystDao.get(url);
    }

    @Override
    public List<Analyst> getActive() {
        return analystDao.getActive();
    }

    @Override
    public List<Analyst> getAll() {
        return analystDao.getAll();
    }

    @Override
    public AnalystClient getAnalystClient() throws Exception {

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        InputStream trustStoreInput = new ClassPathResource("truststore.p12").getInputStream();
        trustStore.load(trustStoreInput, "zorroa" .toCharArray());

        AnalystClient client = new AnalystClient(trustStore);
        for (Analyst a:  analystDao.getAll(AnalystState.UP)) {
            client.getLoadBalancer().addHost(a.getUrl());
        }
        return client;
    }

    @Override
    public List<ProcessorProperties> getProcessors(ProcessorType type) {
        return pluginDao.getProcessors(type);
    }

    @Override
    public List<ProcessorProperties> getProcessors() {
        return pluginDao.getProcessors();
    }
}
