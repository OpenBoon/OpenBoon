package com.zorroa.archivist.domain;

import java.util.Set;

/**
 * Created by chambers on 7/11/15.
 */
public class IngestUpdateBuilder {

    private String path;
    private Set<String> fileTypes;
    private String pipeline;
    private int pipelineId = -1;
    private boolean updateOnExist;
    private int assetWorkerThreads = -1;

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

    public boolean isUpdateOnExist() {
        return updateOnExist;
    }

    public void setUpdateOnExist(boolean updateOnExist) {
        this.updateOnExist = updateOnExist;
    }

    public int getAssetWorkerThreads() {
        return assetWorkerThreads;
    }

    public void setAssetWorkerThreads(int assetWorkerThreads) {
        this.assetWorkerThreads = assetWorkerThreads;
    }
}
