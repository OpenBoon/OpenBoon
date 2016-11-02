package com.zorroa.archivist.service;

import com.zorroa.archivist.AnalystClient;
import com.zorroa.common.config.ApplicationProperties;
import com.zorroa.common.repository.AnalystDao;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by chambers on 2/9/16.
 */
@Service
public class AnalystServiceImpl implements AnalystService {

    @Autowired
    AnalystDao analystDao;

    @Autowired
    ApplicationProperties properties;

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
        return analystDao.getActive(new Pager(1, 100));
    }

    @Override
    public PagedList<Analyst> getAll(Pager paging) {
        return analystDao.getAll(paging);
    }

    @Override
    public AnalystClient getAnalystClient(String host) {
        AnalystClient client = new AnalystClient();
        client.getLoadBalancer().addHost(host);
        return client;
    }

    @Override
    public AnalystClient getAnalystClient() {
        AnalystClient client = new AnalystClient();
        for (Analyst a : analystDao.getActive(new Pager(1, 5), maxQueueSize)) {
            client.getLoadBalancer().addHost(a.getUrl());
        }
        return client;
    }
}
