package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.IngestPipeline;
import com.zorroa.sdk.domain.IngestPipelineBuilder;
import com.zorroa.sdk.domain.IngestPipelineUpdateBuilder;

import java.util.List;

/**
 * Created by chambers on 7/7/16.
 */
public interface PipelineService {
    IngestPipeline createIngestPipeline(IngestPipelineBuilder builder);

    IngestPipeline getIngestPipeline(int id);

    IngestPipeline getIngestPipeline(String s);

    boolean ingestPipelineExists(String s);

    List<IngestPipeline> getIngestPipelines();

    boolean updateIngestPipeline(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean deleteIngestPipeline(IngestPipeline pipeline);
}
