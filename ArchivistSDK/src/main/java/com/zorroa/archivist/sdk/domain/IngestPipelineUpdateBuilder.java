package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.util.List;

/**
 * Created by chambers on 7/12/15.
 */
public class IngestPipelineUpdateBuilder {

    private List<ProcessorFactory<IngestProcessor>> processors;
    private List<ProcessorFactory<IngestProcessor>> aggregators;
    private String name;
    private String description;

    public List<ProcessorFactory<IngestProcessor>> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorFactory<IngestProcessor>> processors) {
        this.processors = processors;
    }

    public List<ProcessorFactory<IngestProcessor>> getAggregators() {
        return aggregators;
    }

    public void setAggregators(List<ProcessorFactory<IngestProcessor>> aggregators) {
        this.aggregators = aggregators;
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
