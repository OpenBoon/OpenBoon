package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 7/11/16.
 */
public class ImportSpec {

    @NotEmpty
    private UnresolvedModule generator;

    /**
     * Attributes to manually assign.
     */
    public Map<String, Object> attrs = ImmutableMap.of();

    /**
     * Any global args to the script
     */
    public Map<String, Object> args = ImmutableMap.of();

    /**
     * A custom pipeline to run the assets through. Can be null.
     */
    public List<UnresolvedModule> pipeline;

    /**
     * Utilize a pre-existing pipline.
     */
    public Integer pipelineId;


    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public ImportSpec setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    public List<UnresolvedModule> getPipeline() {
        return pipeline;
    }

    public ImportSpec setPipeline(List<UnresolvedModule> pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public ImportSpec setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public UnresolvedModule getGenerator() {
        return generator;
    }

    public ImportSpec setGenerator(UnresolvedModule generator) {
        this.generator = generator;
        return this;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public ImportSpec setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }
}
