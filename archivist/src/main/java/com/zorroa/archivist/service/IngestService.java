package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.*;

import java.util.List;
import java.util.Set;

public interface IngestService {

    /*
    * INGEST PIPELINE
    */
    IngestPipeline createIngestPipeline(IngestPipelineBuilder builder);

    List<IngestPipeline> getIngestPipelines();

    IngestPipeline getIngestPipeline(int id);

    IngestPipeline getIngestPipeline(String name);

    boolean ingestPipelineExists(String name);

    void updateIngestStartTime(Ingest ingest, long time);

    void updateIngestStopTime(Ingest ingest, long time);

    /*
     * INGEST
     */
    Ingest createIngest(IngestBuilder builder);

    boolean deleteIngest(Ingest ingest);

    boolean updateIngest(Ingest ingest, IngestUpdateBuilder builder);

    Ingest getIngest(int id);

    List<Ingest> getAllIngests();

    List<Ingest> getIngests(IngestFilter filter);

    List<Ingest> getAllIngests(IngestState state, int limit);

    boolean updateIngestPipeline(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean deleteIngestPipeline(IngestPipeline pipeline);

    void setTotalAssetCount(Ingest ingest, long count);

    void incrementIngestCounters(Ingest ingest, int created, int updated, int errors, int warnings);

    boolean setIngestRunning(Ingest ingest);

    void resetIngestCounters(Ingest ingest);

    boolean setIngestIdle(Ingest ingest);

    boolean setIngestQueued(Ingest ingest);

    boolean setIngestPaused(Ingest ingest, boolean value);

    void beginWorkOnPath(Ingest ingest, String path);

    void endWorkOnPath(Ingest ingest, String path);

    Set<String> getSkippedPaths(Ingest ingest);

    /**
     * Get the folder linked to the given ingest.
     *
     * @param ingest
     * @return
     */
    Folder getFolder(Ingest ingest);
}
