package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableMap;
import com.zorroa.sdk.processor.ProcessorRef;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 7/11/16.
 */
public class ImportSpec {

    private String name;

    @NotEmpty
    private List<ProcessorRef> generators;

    /**
     * Any global args to the script
     */
    public Map<String, Object> args = ImmutableMap.of();

    /**
     * A custom pipeline to run the assets through. Can be null.
     */
    public List<ProcessorRef> pipeline;

    /**
     * Utilize a pre-existing pipline.
     */
    public Integer pipelineId;

    public List<ProcessorRef> getPipeline() {
        return pipeline;
    }

    public ImportSpec setPipeline(List<ProcessorRef> pipeline) {
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

    public Map<String, Object> getArgs() {
        return args;
    }

    public ImportSpec setArgs(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    public String getName() {
        return name;
    }

    public ImportSpec setName(String name) {
        this.name = name;
        return this;
    }

    public List<ProcessorRef> getGenerators() {
        return generators;
    }

    public ImportSpec setGenerators(List<ProcessorRef> generators) {
        this.generators = generators;
        return this;
    }
}
