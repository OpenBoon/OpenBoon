package com.zorroa.archivist.repository;

import com.zorroa.sdk.domain.Pipeline;
import com.zorroa.sdk.domain.PipelineBuilder;
import com.zorroa.sdk.domain.PipelineUpdateBuilder;

import java.util.List;

public interface PipelineDao {

    Pipeline create(PipelineBuilder pipeline);

    Pipeline get(String name);

    boolean exists(String name);

    List<Pipeline> getAll();

    Pipeline get(int id);

    boolean update(Pipeline pipeline, PipelineUpdateBuilder builder);

    boolean delete(Pipeline pipeline);
}
