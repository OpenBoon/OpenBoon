package com.zorroa.archivist.service;

import java.util.List;

import com.zorroa.archivist.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.zorroa.archivist.repository.AssetDao;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.repository.ProxyConfigDao;

/**
 *
 * The ingest service is responsible for processing IngestRequest objects which
 * results in the creation of assets using the assetService.
 *
 * @author chambers
 *
 */
@Component
public class IngestServiceImpl implements IngestService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(IngestServiceImpl.class);

    ApplicationContext applicationContext;

    @Autowired
    IngestPipelineDao ingestPipelineDao;

    @Autowired
    IngestDao ingestDao;

    @Autowired
    ProxyConfigDao proxyConfigDao;

    @Override
    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        return ingestPipelineDao.create(builder);
    }

    @Override
    public IngestPipeline getIngestPipeline(String name) {
        return ingestPipelineDao.get(name);
    }

    @Override
    public IngestPipeline getIngestPipeline(int id) {
        return ingestPipelineDao.get(id);
    }

    @Override
    public List<IngestPipeline> getIngestPipelines() {
        return ingestPipelineDao.getAll();
    }

    @Override
    public boolean setIngestRunning(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Running, IngestState.Queued);
    }

    @Override
    public boolean setIngestIdle(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Idle, IngestState.Running);
    }

    @Override
    public boolean setIngestQueued(Ingest ingest) {
        return ingestDao.setState(ingest, IngestState.Queued, IngestState.Idle);
    }

    @Override
    public Ingest createIngest(IngestBuilder builder) {
        IngestPipeline pipeline = ingestPipelineDao.get(builder.getPipeline());
        ProxyConfig proxyConfig = proxyConfigDao.get(builder.getProxyConfig());
        return ingestDao.create(pipeline, proxyConfig, builder);
    }

    @Override
    public Ingest getIngest(long id) {
        return ingestDao.get(id);
    }

    @Override
    public List<Ingest> getAllIngests() {
        return ingestDao.getAll();
    }

    @Override
    public List<Ingest> getAllIngests(IngestState state, int limit) {
        return ingestDao.getAll(state, limit);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }
}

