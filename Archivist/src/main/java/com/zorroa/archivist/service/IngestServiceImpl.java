package com.zorroa.archivist.service;

import com.zorroa.archivist.event.EventServerHandler;
import com.zorroa.archivist.repository.IngestDao;
import com.zorroa.archivist.repository.IngestPipelineDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.IngestService;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@Transactional
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

    @Autowired
    FolderService folderService;

    @Override
    public IngestPipeline createIngestPipeline(IngestPipelineBuilder builder) {
        return ingestPipelineDao.create(builder);
    }

    @Override
    public IngestPipeline getIngestPipeline(int id) {
        return ingestPipelineDao.get(id);
    }

    @Override
    public IngestPipeline getIngestPipeline(String s) {
        return ingestPipelineDao.get(s);
    }

    @Override
    public boolean ingestPipelineExists(String s) {
        return ingestPipelineDao.exists(s);
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
        if (ingestDao.setState(ingest, IngestState.Running)) {
            broadcast(ingest, MessageType.INGEST_UPDATE);
            return true;
        }
        return false;
    }

    @Override
    public void resetIngestCounters(Ingest ingest) {
        ingestDao.resetCounters(ingest);
    }

    @Override
    public boolean setIngestIdle(Ingest ingest) {
        if (ingestDao.setState(ingest, IngestState.Idle)) {
            ingestDao.updateStoppedTime(ingest, System.currentTimeMillis());
            broadcast(ingest, MessageType.INGEST_UPDATE);
            return true;
        }
        return false;
    }

    @Override
    public boolean setIngestQueued(Ingest ingest) {
        if (ingestDao.setState(ingest, IngestState.Queued)) {
            broadcast(ingest, MessageType.INGEST_UPDATE);
            return true;
        }
        return false;
    }

    @Override
    public boolean setIngestPaused(Ingest ingest) {
        if (ingestDao.setState(ingest, IngestState.Paused)) {
            broadcast(ingest, MessageType.INGEST_UPDATE);
            return true;
        }
        return false;
    }

    private void broadcast(Ingest ingest, MessageType messageType) {
        /*
         * Pulling an updated copy of the ingest to account for any changed fields.
         */
        try {
            ingest = getIngest(ingest.getId());
        } catch (EmptyResultDataAccessException ignore) {

        }
        String json = new String(Json.serialize(ingest), StandardCharsets.UTF_8);
        eventServerHandler.broadcast(new Message(messageType, json));
    }

    @Override
    public void incrementIngestCounters(Ingest ingest, int created, int updated, int errors) {
        ingestDao.incrementCounters(ingest, created, updated, errors);
        broadcast(ingest, MessageType.INGEST_UPDATE_COUNTERS);
    }

    @Override
    public void updateIngestStartTime(Ingest ingest, long time) {
        ingest.setTimeStarted(time);
        broadcast(ingest, MessageType.INGEST_START);
    }

    @Override
    public void updateIngestStopTime(Ingest ingest, long time) {
        ingest.setTimeStopped(time);
        broadcast(ingest, MessageType.INGEST_STOP);
    }

    @Override
    public Ingest createIngest(IngestBuilder builder) {
        IngestPipeline pipeline;
        if (builder.getPipelineId() == -1) {
            pipeline = ingestPipelineDao.get("standard");
        }
        else {
            pipeline = ingestPipelineDao.get(builder.getPipelineId());
        }
        Ingest ingest = ingestDao.create(pipeline, builder);
        broadcast(ingest, MessageType.INGEST_CREATE);

        // Create ingest folder asynchronously to avoid event dependency loop,
        // but don't wait for processing since clients see the folder
        new Thread(new Runnable() { public void run() { getIngestFolder(ingest); }}).start();

        return ingest;
    }

    @Override
    public boolean deleteIngest(Ingest ingest) {
        new Thread(new Runnable() { public void run() { folderService.delete(getIngestFolder(ingest)); }}).start();
        boolean ok = ingestDao.delete(ingest);
        broadcast(ingest, MessageType.INGEST_DELETE);
        return ok;
    }

    @Override
    public boolean updateIngest(Ingest ingest, IngestUpdateBuilder builder) {

        // Update active ingest thread counts
        if (ingest.getState() == IngestState.Running && builder.getAssetWorkerThreads() > 0 &&
                ingest.getAssetWorkerThreads() != builder.getAssetWorkerThreads()) {

            synchronized(ingest) {
                ingest.setAssetWorkerThreads(builder.getAssetWorkerThreads());
                ingestExecutorService.pause(ingest);
                ingestExecutorService.resume(ingest);
            }
        }

        if (builder.getPipeline() != null) {
            builder.setPipelineId(ingestPipelineDao.get(builder.getPipelineId()).getId());
        }

        boolean ok = ingestDao.update(ingest, builder);
        broadcast(ingest, MessageType.INGEST_UPDATE);
        return ok;
    }

    @Override
    public Ingest getIngest(int id) {
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

    @Override
    public Folder getIngestFolder(Ingest ingest) {
        synchronized (folderService) {
            // Create the root folder for all ingests, if needed
            Folder ingestsFolder;
            try {
                ingestsFolder = folderService.get(0, "Ingests");
            } catch (EmptyResultDataAccessException e) {
                ingestsFolder = folderService.create(new FolderBuilder().setName("Ingests"));
            }

            // Create a root folder for this ingest (watch out for name with slashes!)
            String name = Integer.toString(ingest.getId()) + ":" + ingest.getPath();
            try {
                return folderService.get(ingestsFolder.getId(), name);
            } catch (EmptyResultDataAccessException e) {
                // Folder does not exist, create a new one
                int ingestId = (int) ingest.getId();     // FIXME: int-cast from long is dangerous
                AssetFilter ingestFilter = new AssetFilter().setIngestId(ingestId);
                FolderBuilder ingestBuilder = new FolderBuilder().setName(name).setParentId(ingestsFolder.getId())
                        .setSearch(new AssetSearch().setFilter(ingestFilter));
                return folderService.create(ingestBuilder);
            }
        }
    }
}

