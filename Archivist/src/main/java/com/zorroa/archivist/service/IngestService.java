package com.zorroa.archivist.service;

import java.util.List;

import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;

public interface IngestService {

    IngestPipeline createIngestPipeline(IngestPipelineBuilder builder);

    IngestPipeline getIngestPipeline(String id);

    Ingest createIngest(IngestBuilder builder);

    List<IngestPipeline> getIngestPipelines();

    Ingest getNextWaitingIngest();

    IngestPipeline getIngestPipeline(int id);

    void incrementCreatedCount(Ingest ingest, int increment);

    Ingest getIngest(long id);

    List<Ingest> getPendingIngests();

    void incrementErrorCount(Ingest ingest, int increment);

    boolean setIngestRunning(Ingest ingest);

    boolean setIngestFinished(Ingest ingest);
}
