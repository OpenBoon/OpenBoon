package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.IngestPipeline;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestPipelineUpdateBuilder;

import java.util.List;

public interface IngestPipelineDao {

    IngestPipeline create(IngestPipelineBuilder pipeline);

    List<IngestPipeline> getAll();

    IngestPipeline get(int id);

    boolean update(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean delete(IngestPipeline pipeline);
}
