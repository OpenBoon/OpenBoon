package com.zorroa.archivist.service;

import java.util.List;

import com.zorroa.archivist.domain.*;

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

    Ingest getIngest(long id);

    List<Ingest> getAllIngests();

    List<Ingest> getIngests(IngestFilter filter);

    List<Ingest> getAllIngests(IngestState state, int limit);

    boolean setIngestRunning(Ingest ingest);

    boolean setIngestIdle(Ingest ingest);

    boolean setIngestQueued(Ingest ingest);

}
