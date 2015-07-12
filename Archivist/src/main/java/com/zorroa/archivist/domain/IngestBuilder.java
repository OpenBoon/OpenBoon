package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.elasticsearch.common.collect.ImmutableSet;

import java.util.Set;

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

    public IngestBuilder setProxyConfig(String proxyConfig) {
        this.proxyConfig = proxyConfig;
        return this;
    }

    public String getPipeline() {
        return pipeline;
    }

    public IngestBuilder setPipeline(String pipeline) {
        this.pipeline = pipeline;
        return this;
    }
}
