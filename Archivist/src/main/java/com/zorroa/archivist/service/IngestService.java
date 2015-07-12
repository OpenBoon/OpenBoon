package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.*;

import java.util.List;

public interface IngestService {

    /*
    * INGEST PIPELINE
    */
    IngestPipeline createIngestPipeline(IngestPipelineBuilder builder);

    IngestPipeline getIngestPipeline(String id);

    List<IngestPipeline> getIngestPipelines();

    IngestPipeline getIngestPipeline(int id);


    /*
    * INGEST
    */
    Ingest createIngest(IngestBuilder builder);

    boolean updateIngest(Ingest ingest, IngestUpdateBuilder builder);

    Ingest getIngest(long id);

    List<Ingest> getAllIngests();

    List<Ingest> getIngests(IngestFilter filter);

    List<Ingest> getAllIngests(IngestState state, int limit);

    boolean updateIngestPipeline(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean setIngestRunning(Ingest ingest);

    boolean setIngestIdle(Ingest ingest);

    boolean setIngestQueued(Ingest ingest);

}
