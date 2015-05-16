package com.zorroa.archivist.domain;

import java.util.List;

public class IngestPipelineBuilder {

    private List<IngestProcessorWrapper> processors;
    private String name;

    public IngestPipelineBuilder() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public List<IngestProcessorWrapper> getProcessors() {
        return processors;
    }

    public void setProcessors(List<IngestProcessorWrapper> processors) {
        this.processors = processors;
    }
}
