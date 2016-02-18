package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.util.List;

/**
 * Created by chambers on 2/8/16.
 */
public class AnalyzeRequest implements EventLoggable {
    private Integer ingestId;
    private Integer ingestPipelineId;
    private List<String> paths;
    private String user;
    private List<ProcessorFactory<IngestProcessor>> processors;

    public List<ProcessorFactory<IngestProcessor>> getProcessors() {
        return processors;
    }

    public AnalyzeRequest setProcessors(List<ProcessorFactory<IngestProcessor>> processors) {
        this.processors = processors;
        return this;
    }

    public String getUser() {
        return user;
    }

    public AnalyzeRequest setUser(String user) {
        this.user = user;
        return this;
    }

    public List<String> getPaths() {
        return paths;
    }

    public AnalyzeRequest setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public Integer getIngestId() {
        return ingestId;
    }

    public AnalyzeRequest setIngestId(Integer ingestId) {
        this.ingestId = ingestId;
        return this;
    }

    public Integer getIngestPipelineId() {
        return ingestPipelineId;
    }

    public AnalyzeRequest setIngestPipelineId(Integer ingestPipelineId) {
        this.ingestPipelineId = ingestPipelineId;
        return this;
    }

    @Override
    public Object getLogId() {
        return ingestId;
    }

    @Override
    public String getLogType() {
        return "Ingest";
    }
}
