package com.zorroa.archivist.domain;

import java.util.List;

public class IngestPipelineBuilder {

    private List<IngestProcessorFactory> processors;
    private String name;

    public IngestPipelineBuilder() { }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addToProcessors(IngestProcessorFactory processor) {
        processors.add(processor);
    }

    public List<IngestProcessorFactory> getProcessors() {
        return processors;
    }

    public void setProcessors(List<IngestProcessorFactory> processors) {
        this.processors = processors;
    }
}
