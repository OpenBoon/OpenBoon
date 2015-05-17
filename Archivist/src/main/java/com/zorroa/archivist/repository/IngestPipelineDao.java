package com.zorroa.archivist.repository;

import java.util.List;

import com.zorroa.archivist.domain.IngestPipeline;
import com.zorroa.archivist.domain.IngestPipelineBuilder;

public interface IngestPipelineDao {

    String create(IngestPipelineBuilder pipeline);

    IngestPipeline get(String id);

    List<IngestPipeline> getAll();
}
