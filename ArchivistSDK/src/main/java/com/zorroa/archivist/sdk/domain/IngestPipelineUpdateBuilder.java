package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Created by chambers on 7/12/15.
 */
public class IngestPipelineUpdateBuilder {

    private List<IngestProcessorFactory> processors;
    private String name;
    private String description;

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
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
