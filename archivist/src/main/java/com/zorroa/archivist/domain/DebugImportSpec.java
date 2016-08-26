package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * The DebugImportSpec class is specifically for doing debug imports.  This class only
 * allows a single path to be analyzed.
 */
public class DebugImportSpec {

    @NotEmpty
    private String path;

    private List<ProcessorRef> pipeline;

    /**
     * Utilize a pre-existing pipeline.  This could be the name or a numeric id.
     */
    public Object pipelineId;

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

    public Object getPipelineId() {
        return pipelineId;
    }

    public DebugImportSpec setPipelineId(Object pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }
}
