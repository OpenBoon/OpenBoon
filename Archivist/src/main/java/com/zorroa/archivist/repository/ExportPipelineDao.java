package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.ExportPipeline;
import com.zorroa.archivist.sdk.domain.ExportPipelineBuilder;

/**
 * Created by chambers on 11/2/15.
 */
public interface ExportPipelineDao {
    ExportPipeline create(ExportPipelineBuilder builder);

    ExportPipeline get(int id);
}
