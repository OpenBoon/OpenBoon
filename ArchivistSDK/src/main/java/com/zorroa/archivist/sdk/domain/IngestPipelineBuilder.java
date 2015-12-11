package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.util.List;

public class IngestPipelineBuilder {

    private List<ProcessorFactory<IngestProcessor>> processors;
    private String name;
    private String description;

    public IngestPipelineBuilder() {
        processors = Lists.newArrayList();
    }

    public void addToProcessors(ProcessorFactory<IngestProcessor> processor) {
        processors.add(processor);
    }

    public List<ProcessorFactory<IngestProcessor>> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorFactory<IngestProcessor>> processors) {
        this.processors = processors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        if (description == null) {
            return "";
        }
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
