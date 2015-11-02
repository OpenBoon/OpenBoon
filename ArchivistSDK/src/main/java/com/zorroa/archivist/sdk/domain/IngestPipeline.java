package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.util.List;


public class IngestPipeline {

    private int id;
    private String name;
    private String description;
    private long timeCreated;
    private String userCreated;
    private long timeModified;
    private String userModified;
    private List<ProcessorFactory<IngestProcessor>> processors;

    public IngestPipeline() { }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(String userCreated) {
        this.userCreated = userCreated;
    }

    public long getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }

    public String getUserModified() {
        return userModified;
    }

    public void setUserModified(String userModified) {
        this.userModified = userModified;
    }

    public List<ProcessorFactory<IngestProcessor>> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorFactory<IngestProcessor>> processors) {
        this.processors = processors;
    }
}
