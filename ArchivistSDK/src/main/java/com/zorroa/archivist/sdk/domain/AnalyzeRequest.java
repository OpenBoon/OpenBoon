package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 2/8/16.
 */
public class AnalyzeRequest implements EventLoggable {
    private int ingestId;
    private int ingestPipelineId;
    private List<String> paths;
    private String user;
    private List<ProcessorFactory<IngestProcessor>> processors;
    private Map<String, Object> fileSystemArgs;

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

    public int getIngestId() {
        return ingestId;
    }

    public AnalyzeRequest setIngestId(int ingestId) {
        this.ingestId = ingestId;
        return this;
    }

    public int getIngestPipelineId() {
        return ingestPipelineId;
    }

    public AnalyzeRequest setIngestPipelineId(int ingestPipelineId) {
        this.ingestPipelineId = ingestPipelineId;
        return this;
    }

    public Map<String, Object> getFileSystemArgs() {
        return fileSystemArgs;
    }

    public AnalyzeRequest setFileSystemArgs(Map<String, Object> fileSystemArgs) {
        this.fileSystemArgs = fileSystemArgs;
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
