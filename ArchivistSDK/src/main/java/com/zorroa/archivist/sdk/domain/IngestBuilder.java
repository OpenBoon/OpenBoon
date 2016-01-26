package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;

import java.util.List;

public class IngestBuilder {
    private String name;
    private List<String> paths;
    private int pipelineId = -1;
    private int assetWorkerThreads = 4;

    public IngestBuilder() { }

    public IngestBuilder(String path) {
        this.paths = Lists.newArrayList(path);
    }

    public List<String> getPaths() {
        return paths;
    }

    public IngestBuilder setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public IngestBuilder addToPaths(String path) {
        if (paths == null) {
            paths = Lists.newArrayList();
        }
        paths.add(path);
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
