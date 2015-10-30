package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.*;

import java.util.List;

public interface IngestService {

    /*
    * INGEST PIPELINE
    */
    IngestPipeline createIngestPipeline(IngestPipelineBuilder builder);

    List<IngestPipeline> getIngestPipelines();

    IngestPipeline getIngestPipeline(int id);

    void updateIngestCounters(Ingest ingest, int created, int updated, int errors);

    void updateIngestStartTime(Ingest ingest, long time);

    void updateIngestStopTime(Ingest ingest, long time);

    /*
     * INGEST
     */
    Ingest createIngest(IngestBuilder builder);

    boolean deleteIngest(Ingest ingest);

    boolean updateIngest(Ingest ingest, IngestUpdateBuilder builder);

    Ingest getIngest(long id);

    List<Ingest> getAllIngests();

    List<Ingest> getIngests(IngestFilter filter);

    List<Ingest> getAllIngests(IngestState state, int limit);

    boolean updateIngestPipeline(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean deleteIngestPipeline(IngestPipeline pipeline);

    boolean setIngestRunning(Ingest ingest);

    void resetIngestCounters(Ingest ingest);

    boolean setIngestIdle(Ingest ingest);

    boolean setIngestQueued(Ingest ingest);

    boolean setIngestPaused(Ingest ingest);

}
