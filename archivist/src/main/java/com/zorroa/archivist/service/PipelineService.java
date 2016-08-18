package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;

import java.util.List;

/**
 * Created by chambers on 7/7/16.
 */
public interface PipelineService {
    Pipeline create(PipelineSpecV spec);

    Pipeline get(int id);

    Pipeline get(String name);

    boolean exists(String s);

    List<Pipeline> getAll();

    PagedList<Pipeline> getAll(Paging page);

    boolean update(int id, Pipeline spec);

    boolean delete(int id);
}
