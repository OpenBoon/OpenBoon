package com.zorroa.archivist.domain;

import com.zorroa.sdk.domain.Document;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Created by chambers on 8/22/16.
 */
public class TaskSpecV implements JobId {

    @NotEmpty
    private String name;

    @NotEmpty
    private UUID jobId;

    @NotEmpty
    private List<Document> docs;

    private UUID pipelineId;

    public TaskSpecV() { }

    public TaskSpecV(UUID jobId, String name) {
        this.jobId = jobId;
        this.name = name;
    }

    public UUID getJobId() {
        return jobId;
    }

    public TaskSpecV setJobId(UUID jobId) {
        this.jobId = jobId;
        return this;
    }

    public String getName() {
        return name;
    }

    public TaskSpecV setName(String name) {
        this.name = name;
        return this;
    }
    public UUID getPipelineId() {
        return pipelineId;
    }

    public TaskSpecV setPipelineId(UUID pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public List<Document> getDocs() {
        return docs;
    }

    public TaskSpecV setDocs(List<Document> docs) {
        this.docs = docs;
        return this;
    }
}
