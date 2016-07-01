package com.zorroa.archivist.service;

import com.zorroa.common.domain.Paging;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.client.analyst.AnalystClient;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.exception.ArchivistException;
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

    @Value("${archivist.scheduler.maxQueueSize}")
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
