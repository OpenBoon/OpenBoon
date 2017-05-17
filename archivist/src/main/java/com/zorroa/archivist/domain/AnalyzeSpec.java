package com.zorroa.archivist.domain;

import com.zorroa.sdk.processor.ProcessorRef;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class AnalyzeSpec {

    /**
     * The ID of an existing asset.
     */
    private String asset;

    /**
     * Utilize an anonymous pipeline.
     */
    @Nullable
    private List<ProcessorRef> pipeline;

    /**
     * Utilize a pre-existing pipeline.  This could be the name or a numeric id.
     */
    @Nullable
    private Object pipelineId;

    @Nullable
    private Map<String, Object> args;

    public String getAsset() {
        return asset;
    }

    public AnalyzeSpec setAsset(String asset) {
        this.asset = asset;
        return this;
    }

    public List<ProcessorRef> getPipeline() {
        return pipeline;
    }

    public AnalyzeSpec setPipeline(List<ProcessorRef> pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public Object getPipelineId() {
        return pipelineId;
    }

    public AnalyzeSpec setPipelineId(Object pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public AnalyzeSpec setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }
}
