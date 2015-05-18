package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.IngestBuilder;
import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;

public interface IngestService {

    IngestPipeline createIngestPipeline(IngestPipelineBuilder builder);

    IngestPipeline getIngestPipeline(String id);

    void ingest(IngestPipeline pipeline, IngestBuilder builder);
}
