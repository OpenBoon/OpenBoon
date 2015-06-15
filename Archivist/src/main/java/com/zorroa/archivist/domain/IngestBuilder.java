package com.zorroa.archivist.domain;

import java.util.Set;

import org.elasticsearch.common.collect.ImmutableSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class IngestBuilder {

    private String path;
    private Set<String> fileTypes = ImmutableSet.<String>of();
    private String proxyConfig = "standard";
    private String pipeline = "standard";

    public IngestBuilder() { }

    public IngestBuilder(String path) {
        this.path = path;
    }

    public IngestBuilder(String path, Set<String> fileTypes) {
        this.path = path;
        this.fileTypes = fileTypes;
    }

    public Set<String> getFileTypes() {
        return fileTypes;
    }

    @JsonIgnore
    public boolean isSupportedFileType(String type) {
        if (fileTypes.isEmpty()) {
            return true;
        }

        return fileTypes.contains(type);
    }

    public IngestBuilder setFileTypes(Set<String> fileTypes) {
        this.fileTypes = fileTypes;
        return this;
    }

    public String getPath() {
        return path;
    }

    public IngestBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public String getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(String proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public String getPipeline() {
        return pipeline;
    }

    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
}
