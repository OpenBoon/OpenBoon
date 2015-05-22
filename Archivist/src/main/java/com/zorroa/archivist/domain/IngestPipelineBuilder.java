package com.zorroa.archivist.domain;

import java.util.List;

import org.elasticsearch.common.collect.Lists;

public class IngestPipelineBuilder {

    private List<IngestProcessorFactory> processors;
    private String id;

    public IngestPipelineBuilder() { }

    public void addToProcessors(IngestProcessorFactory processor) {
        if (processors == null) {
            processors = Lists.newArrayList();
        }
        processors.add(processor);
    }

    public List<IngestProcessorFactory> getProcessors() {
        return processors;
    }

    public void setProcessors(List<IngestProcessorFactory> processors) {
        this.processors = processors;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
