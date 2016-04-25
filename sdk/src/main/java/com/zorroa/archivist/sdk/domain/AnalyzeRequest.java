package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;

import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * Created by chambers on 2/8/16.
 */
public class AnalyzeRequest implements EventLoggable {
    private Integer ingestId;
    private Integer ingestPipelineId;
    private String user;
    private List<AnalyzeRequestEntry> assets = Lists.newArrayList();
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

    public List<AnalyzeRequestEntry> getAssets() {
        return assets;
    }

    public AnalyzeRequest setAssets(List<AnalyzeRequestEntry> assets) {
        this.assets = assets;
        return this;
    }

    public AnalyzeRequest addToAssets(String path) {
        this.assets.add(new AnalyzeRequestEntry(URI.create(path)));
        return this;
    }

    public AnalyzeRequest addToAssets(File file) {
        this.assets.add(new AnalyzeRequestEntry(file.getAbsoluteFile().toURI()));
        return this;
    }

    @JsonIgnore
    public int getAssetCount() {
        return assets.size();
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
