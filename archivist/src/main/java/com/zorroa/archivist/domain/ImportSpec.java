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
    private List<ProcessorRef> processors;

    private int batchSize = 25;

    public List<ProcessorRef> getProcessors() {
        return processors;
    }

    public ImportSpec setProcessors(List<ProcessorRef> processors) {
        this.processors = processors;
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

    public int getBatchSize() {
        return batchSize;
    }

    public ImportSpec setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }
}
