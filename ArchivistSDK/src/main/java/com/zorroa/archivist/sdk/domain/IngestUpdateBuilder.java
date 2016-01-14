package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 7/11/15.
 */
public class IngestUpdateBuilder {

    private String name;
    private List<String> paths;
    private String pipeline;
    private int pipelineId = -1;
    private int assetWorkerThreads = -1;

    private Set<String> isset = Sets.newHashSet();

    public List<String> getPaths() {
        return paths;
    }

    public String getName() {
        return name;
    }

    public IngestUpdateBuilder setName(String name) {
        this.name = name;
        isset.add("name");
        return this;
    }

    public IngestUpdateBuilder setPaths(List<String> path) {
        this.paths = path;
        isset.add("paths");
        return this;
    }

    public IngestUpdateBuilder addToPaths(String path) {
        if (this.paths == null) {
            this.paths = Lists.newArrayList();
        }
        this.paths.add(path);
        isset.add("paths");
        return this;
    }


    public String getPipeline() {
        return pipeline;
    }

    public IngestUpdateBuilder setPipeline(String pipeline) {
        this.pipeline = pipeline;
        return this;
    }

    public int getPipelineId() {
        return pipelineId;
    }

    public IngestUpdateBuilder setPipelineId(int pipelineId) {

        this.pipelineId = pipelineId;
        isset.add("pipelineId");
        return this;
    }

    public int getAssetWorkerThreads() {
        return assetWorkerThreads;
    }

    public IngestUpdateBuilder setAssetWorkerThreads(int assetWorkerThreads) {
        this.assetWorkerThreads = assetWorkerThreads;
        isset.add("assetWorkerThreads");
        return this;
    }

    public Set<String> getIsset() {
        return isset;
    }

    public IngestUpdateBuilder setIsset(Set<String> isset) {
        this.isset = isset;
        return this;
    }

    public boolean isset(String name) {
        return isset.contains(name);
    }
}
