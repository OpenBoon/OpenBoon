package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.exception.ArchivistException;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@Service
public class AnalystServiceImpl implements AnalystService {

    @Autowired
    AnalystDao analystDao;

    @Value("${archivist.ingest.maxQueueSize}")
    private int maxQueueSize;

    @Override
    public Analyst get(String url) {
        return analystDao.get(url);
    }

    @Override
    public int getCount() {
        return Math.toIntExact(analystDao.count());
    }

    @Override
    public List<Analyst> getActive() {
        return analystDao.getActive(new Paging(1, 100));
    }

    @Override
    public List<Analyst> getAll(Paging paging) {
        return analystDao.getAll(new Paging(1, 100));
    }

    @Override
    public List<Analyst> getAll() {
        return analystDao.getAll(new Paging(1, 100));
    }

    @Override
    public List<PluginProperties> getPlugins() {
        try {
            Analyst a = analystDao.getActive(new Paging(1, 1)).get(0);
            return a.getPlugins();
        } catch (IndexOutOfBoundsException e) {
            return ImmutableList.of();
        }
    }

    @Override
    public List<ProcessorProperties> getProcessors(ProcessorType type) {
        try {
            Analyst a = analystDao.getActive(new Paging(1, 1)).get(0);
            List<ProcessorProperties> result = Lists.newArrayList();
            for (PluginProperties plugin: a.getPlugins()) {
                for (ProcessorProperties pr: plugin.getProcessors()) {
                    if (pr.getType().equals(type)) {
                        result.add(pr);
                    }
                }
            }
            return result;
        } catch (IndexOutOfBoundsException e) {
            //ignore
        }
        return ImmutableList.of();
    }

    @Override
    public List<ProcessorProperties> getProcessors() {
        try {
            Analyst a = analystDao.getActive(new Paging(1, 1)).get(0);
            List<ProcessorProperties> result = Lists.newArrayList();
            for (PluginProperties plugin: a.getPlugins()) {
               result.addAll(plugin.getProcessors());
            }
            return result;
        } catch (ArrayIndexOutOfBoundsException e) {
            //ignore
        }
        return ImmutableList.of();
    }

    @Override
    public AnalystClient getAnalystClient() {
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance("PKCS12");
            InputStream trustStoreInput = new ClassPathResource("truststore.p12").getInputStream();
            trustStore.load(trustStoreInput, "zorroa".toCharArray());
        } catch (Exception e) {
            throw new ArchivistException("Failed to acquire SSL client");
        }

        AnalystClient client = new AnalystClient(trustStore);
        for (Analyst a : analystDao.getActive(new Paging(1, 5), maxQueueSize)) {
            client.getLoadBalancer().addHost(a.getUrl());
        }
        return client;
    }
}
