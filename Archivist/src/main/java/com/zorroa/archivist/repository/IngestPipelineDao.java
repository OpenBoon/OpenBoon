package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestPipelineUpdateBuilder;

import java.util.List;

public interface IngestPipelineDao {

    IngestPipeline create(IngestPipelineBuilder pipeline);

    IngestPipeline get(String name);

    List<IngestPipeline> getAll();

    IngestPipeline get(int id);

    boolean update(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder);

    boolean delete(IngestPipeline pipeline);
}
