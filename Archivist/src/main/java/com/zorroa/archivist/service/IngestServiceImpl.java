package com.zorroa.archivist.service;

import com.zorroa.archivist.Json;
import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.IngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

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
    IngestExecutorService ingestExecutorService;

    @Autowired
    EventServerHandler eventServerHandler;

    @Override
    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        return ingestPipelineDao.create(builder);
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
    public boolean updateIngestPipeline(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder) {
        return ingestPipelineDao.update(pipeline, builder);
    }

    @Override
    public boolean deleteIngestPipeline(IngestPipeline pipeline) {
        return ingestPipelineDao.delete(pipeline);
    }

    @Override
    public boolean setIngestRunning(Ingest ingest) {
        ingest.setState(IngestState.Running);
        return ingestDao.setState(ingest, IngestState.Running);
    }

    @Override
    public void resetIngestCounters(Ingest ingest) {
        ingest.setCreatedCount(0);
        ingest.setUpdatedCount(0);
        ingest.setErrorCount(0);
        ingestDao.resetCounters(ingest);
    }

    @Override
    public boolean setIngestIdle(Ingest ingest) {
        if (ingestDao.setState(ingest, IngestState.Idle)) {
            ingest.setState(IngestState.Idle);
            return true;
        }
        return false;
    }

    @Override
    public boolean setIngestQueued(Ingest ingest) {
        if (ingestDao.setState(ingest, IngestState.Queued)) {
            ingest.setState(IngestState.Queued);
            return true;
        }
        return false;
    }

    @Override
    public boolean setIngestPaused(Ingest ingest) {
        if (ingestDao.setState(ingest, IngestState.Paused)) {
            ingest.setState(IngestState.Paused);
            return true;
        }
        return false;
    }

    private void broadcast(Ingest ingest, MessageType messageType) {
        String json = new String(Json.serialize(ingest), StandardCharsets.UTF_8);
        eventServerHandler.broadcast(new Message(messageType, json));
    }

    @Override
    public void updateIngestCounters(Ingest ingest, int created, int updated, int errors) {
        ingest.setCreatedCount(created);
        ingest.setUpdatedCount(updated);
        ingest.setErrorCount(errors);
        ingestDao.updateCounters(ingest, created, updated, errors);
        broadcast(ingest, MessageType.INGEST_UPDATE_COUNTERS);
    }

    @Override
    public void updateIngestStartTime(Ingest ingest, long time) {
        ingest.setTimeStarted(time);
        ingestDao.updateStartTime(ingest, time);
        broadcast(ingest, MessageType.INGEST_START);
    }

    @Override
    public void updateIngestStopTime(Ingest ingest, long time) {
        ingest.setTimeStopped(time);
        ingestDao.updateStoppedTime(ingest, time);
        broadcast(ingest, MessageType.INGEST_STOP);
    }

    @Override
    public Ingest createIngest(IngestBuilder builder) {
        IngestPipeline pipeline = ingestPipelineDao.get(builder.getPipelineId());
        Ingest ingest = ingestDao.create(pipeline, builder);
        broadcast(ingest, MessageType.INGEST_CREATE);
        return ingest;
    }

    @Override
    public boolean deleteIngest(Ingest ingest) {
        boolean ok = ingestDao.delete(ingest);
        broadcast(ingest, MessageType.INGEST_DELETE);
        return ok;
    }

    @Override
    public boolean updateIngest(Ingest ingest, IngestUpdateBuilder builder) {

        // Update active ingest thread counts
        if (ingest.getState() == IngestState.Running && builder.getAssetWorkerThreads() > 0 &&
                ingest.getAssetWorkerThreads() != builder.getAssetWorkerThreads()) {
            ingest.setAssetWorkerThreads(builder.getAssetWorkerThreads());  // set new worker count
            ingestExecutorService.pause(ingest);
            ingestExecutorService.resume(ingest);
        }

        if (builder.getPipeline() != null) {
            builder.setPipelineId(ingestPipelineDao.get(builder.getPipelineId()).getId());
        }

        boolean ok = ingestDao.update(ingest, builder);
        broadcast(ingest, MessageType.INGEST_UPDATE);
        return ok;
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
    public List<Ingest> getIngests(IngestFilter filter) {
        return ingestDao.getAll(filter);
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

