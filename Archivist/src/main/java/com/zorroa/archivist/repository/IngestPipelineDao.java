package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.IngestPipelineBuilder;
import com.zorroa.archivist.domain.IngestPipeline;

public interface IngestPipelineDao {

    String create(IngestPipelineBuilder pipeline);

    IngestPipeline get(String id);

    List<IngestPipeline> getAll();
}
