package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;

import java.util.List;

/**
 * The DebugImportSpec class is specifically for doing debug imports.  This class only
 * allows a single asset to be a
 *
 * At least path or query must be set, as well as either a pipelineId or an anonymous pipeline.
 */
public class DebugImportSpec {

    @Nullable
    private String path;

    @Nullable
    private String query;

    /**
     * Utilize an anonymous pipeline.
     */
    @Nullable
    private List<ProcessorRef> pipeline;

    /**
     * Utilize a pre-existing pipeline.  This could be the name or a numeric id.
     */
    @Nullable
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

    public String getQuery() {
        return query;
    }

    public DebugImportSpec setQuery(String query) {
        this.query = query;
        return this;
    }
}
