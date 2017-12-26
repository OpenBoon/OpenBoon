package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.domain.PipelineType;

public interface PipelineDao extends GenericNamedDao<Pipeline, PipelineSpecV> {

    Pipeline getStandard(PipelineType type);
}
