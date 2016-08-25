package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 8/25/16.
 */
public class DebugImportSpec {

    @NotEmpty
    private String path;

    private List<ProcessorRef> pipeline;

    private String pipelineId;

    public String getPath() {
        return path;
    }

    public DebugImportSpec setPath(String path) {
        this.path = path;
        return this;
    }

    public List<ProcessorRef> getPipeline() {
        return pipeline;
    }

    public DebugImportSpec setPipeline(List<ProcessorRef> pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public DebugImportSpec setPipelineId(String pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }
}
