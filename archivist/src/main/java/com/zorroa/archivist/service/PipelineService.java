package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;

import java.util.List;

/**
 * Created by chambers on 7/7/16.
 */
public interface PipelineService {
    Pipeline create(PipelineSpecV spec);

    Pipeline get(int id);

    Pipeline get(String name);

    Pipeline getStandard();

    boolean exists(String s);

    List<Pipeline> getAll();

    PagedList<Pipeline> getAll(Pager page);

    boolean update(int id, Pipeline spec);

    boolean delete(int id);

    List<ProcessorRef> getProcessors(Object pipelineId, List<ProcessorRef> custom);

    List<ProcessorRef> mungePipelines(List<Object> pipelineIds, List<ProcessorRef> custom);

    boolean isValidPipelineId(Object value);
}
