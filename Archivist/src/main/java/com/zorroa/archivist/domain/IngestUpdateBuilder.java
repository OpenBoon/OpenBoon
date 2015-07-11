package com.zorroa.archivist.domain;

import org.elasticsearch.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by chambers on 7/11/15.
 */
public class IngestUpdateBuilder {

    private String path;
    private Set<String> fileTypes;
    private String proxyConfig;
    private String pipeline;
    private int pipelineId = -1;
    private int proxyConfigId = -1;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Set<String> getFileTypes() {
        return fileTypes;
    }

    public void setFileTypes(Set<String> fileTypes) {
        this.fileTypes = fileTypes;
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

    public int getPipelineId() {
        return pipelineId;
    }

    public void setPipelineId(int pipelineId) {
        this.pipelineId = pipelineId;
    }

    public int getProxyConfigId() {
        return proxyConfigId;
    }

    public void setProxyConfigId(int proxyConfigId) {
        this.proxyConfigId = proxyConfigId;
    }
}
