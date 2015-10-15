package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.elasticsearch.common.collect.ImmutableSet;

import java.util.Set;

public class IngestBuilder {

    private String path;
    private Set<String> fileTypes = ImmutableSet.<String>of();
    private int pipelineId = 1;     // "standard" pipeline is always 1
    private int roomId = 0;
    private boolean updateOnExist = true;
    private int assetWorkerThreads = 4;

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

    public int getPipelineId() {
        return pipelineId;
    }

    public IngestBuilder setPipelineId(int pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
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
