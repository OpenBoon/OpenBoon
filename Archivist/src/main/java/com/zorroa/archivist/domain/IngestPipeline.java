package com.zorroa.archivist.domain;

import java.util.List;

public class IngestPipeline {

    private String id;
    private long version;
    private List<IngestProcessorFactory> processors;

    public IngestPipeline() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<IngestProcessorFactory> getProcessors() {
        return processors;
    }

    public void setProcessors(List<IngestProcessorFactory> processors) {
        this.processors = processors;
    }
}
