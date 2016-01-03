package com.zorroa.archivist.sdk.domain;

public class IngestBuilder {

    private String path;

    /**
     * The ingest pipeline to run.  A -1 ID is the "standard" or default pipeline.
     */
    private int pipelineId = -1;
    private boolean updateOnExist = true;
    private int assetWorkerThreads = 4;

    public IngestBuilder() { }

    public IngestBuilder(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public IngestBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public int getPipelineId() {
        return pipelineId;
    }

    public IngestBuilder setPipelineId(int pipelineId) {
        this.pipelineId = pipelineId;
        return this;
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
