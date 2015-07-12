package com.zorroa.archivist.domain;

import org.elasticsearch.common.collect.Lists;

import java.util.List;

public class IngestPipelineBuilder {

    private List<IngestProcessorFactory> processors;
    private String name;
    private String description;

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
