package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.processor.ProcessorRef;

import java.util.List;

/**
 * Created by chambers on 7/7/16.
 */
public interface PipelineService {
    Pipeline create(PipelineSpecV spec);

    Pipeline get(int id);

    Pipeline get(String name);

    Pipeline getStandard(PipelineType type);

    boolean exists(String s);

    List<Pipeline> getAll();

    PagedList<Pipeline> getAll(Pager page);

    boolean update(int id, Pipeline spec);

    boolean delete(int id);

    List<ProcessorRef> validateProcessors(PipelineType pipelineType, List<ProcessorRef> refs);

    List<ProcessorRef> mungePipelines(PipelineType type, List<ProcessorRef> custom);

    boolean isValidPipelineId(Object value);
}
