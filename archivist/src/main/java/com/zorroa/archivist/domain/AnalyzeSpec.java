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
    private List<ProcessorRef> processors;

    /**
     * Utilize a list of pre-existing pipelines. This could be the name or a numeric id.
     */
    @Nullable
    private List<Object> pipelineIds;

    @Nullable
    private Map<String, Object> args;

    public String getAsset() {
        return asset;
    }

    public AnalyzeSpec setAsset(String asset) {
        this.asset = asset;
        return this;
    }

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public AnalyzeSpec setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
        return this;
    }

    public List<Object> getPipelineIds() {
        return pipelineIds;
    }

    public AnalyzeSpec setPipelineIds(List<Object> pipelineIds) {
        this.pipelineIds = pipelineIds;
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
