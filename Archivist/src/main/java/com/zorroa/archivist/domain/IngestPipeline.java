package com.zorroa.archivist.domain;

import java.util.List;

public class IngestPipeline {

    private String id;
    private long version;

    private String name;
    private List<IngestProcessorWrapper> processors;

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
