package com.zorroa.archivist.domain;

import com.zorroa.sdk.domain.Document;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;

/**
 * Created by chambers on 8/22/16.
 */
public class TaskSpecV implements JobId {

    @NotEmpty
    private String name;

    @NotEmpty
    private Integer jobId;

    @NotEmpty
    private List<Document> docs;

    private Integer pipelineId;

    public TaskSpecV() { }

    public TaskSpecV(int jobId, String name) {
        this.jobId = jobId;
        this.name = name;
    }

    public Integer getJobId() {
        return jobId;
    }

    public TaskSpecV setJobId(Integer jobId) {
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
    public Integer getPipelineId() {
        return pipelineId;
    }

    public TaskSpecV setPipelineId(Integer pipelineId) {
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
