package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;

public interface IngestPipelineDao {

    IngestPipeline create(IngestPipelineBuilder pipeline);

    IngestPipeline get(String name);

    List<IngestPipeline> getAll();

    IngestPipeline get(int id);
}
