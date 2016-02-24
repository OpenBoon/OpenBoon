package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;

import java.util.List;

public class IngestBuilder {
    private String name;
    private List<String> uris;
    private int pipelineId = -1;
    private int assetWorkerThreads = -1;

    public IngestBuilder() { }

    public IngestBuilder(String uri) {
        this.uris = Lists.newArrayList(uri);
    }

    public List<String> getUris() {
        return uris;
    }

    public IngestBuilder setUris(List<String> uris) {
        this.uris = uris;
        return this;
    }

    public IngestBuilder addToUris(String uri) {
        if (uris == null) {
            uris = Lists.newArrayList();
        }
        uris.add(uri);
        return this;
    }


    public int getPipelineId() {
        return pipelineId;
    }

    public IngestBuilder setPipelineId(int pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public int getAssetWorkerThreads() {
        return assetWorkerThreads;
    }

    public void setAssetWorkerThreads(int assetWorkerThreads) {
        this.assetWorkerThreads = assetWorkerThreads;
    }

    public String getName() {
        return name;
    }

    public IngestBuilder setName(String name) {
        this.name = name;
        return this;
    }
}
