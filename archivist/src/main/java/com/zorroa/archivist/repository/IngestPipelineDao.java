package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.IngestPipeline;
import com.zorroa.sdk.domain.IngestPipelineBuilder;
import com.zorroa.sdk.domain.IngestPipelineUpdateBuilder;

import java.util.List;

public interface IngestPipelineDao {

    IngestPipeline create(IngestPipelineBuilder pipeline);

    IngestPipeline get(String name);

    boolean exists(String name);

    List<IngestPipeline> getAll();

    IngestPipeline get(int id);

    boolean update(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean delete(IngestPipeline pipeline);
}
