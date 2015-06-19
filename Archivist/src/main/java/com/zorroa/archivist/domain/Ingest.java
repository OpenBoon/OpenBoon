package com.zorroa.archivist.domain;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Ingest {

    private long id;
    private int pipelineId;
    private int proxyConfigId;
    private IngestState state;
    private String path;
    private Set<String> fileTypes;
    private long timeCreated;
    private String userCreated;
    private long timeModified;
    private String userModified;
    private long timeStopped;
    private long timeStarted;
    private int newAssetCount;
    private int updatedAssetCount;
    private int skippedAssetcount;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public int getPipelineId() {
        return pipelineId;
    }
    public void setPipelineId(int pipelineId) {
        this.pipelineId = pipelineId;
    }
    public IngestState getState() {
        return state;
    }
    public void setState(IngestState state) {
        this.state = state;
    }
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
    public long getTimeCreated() {
        return timeCreated;
    }
    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }
    public String getUserCreated() {
        return userCreated;
    }
    public void setUserCreated(String userCreated) {
        this.userCreated = userCreated;
    }
    public long getTimeModified() {
        return timeModified;
    }
    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }
    public String getUserModified() {
        return userModified;
    }
    public void setUserModified(String userModified) {
        this.userModified = userModified;
    }
    public long getTimeStopped() {
        return timeStopped;
    }
    public void setTimeStopped(long timeStopped) {
        this.timeStopped = timeStopped;
    }
    public long getTimeStarted() {
        return timeStarted;
    }
    public void setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
    }
    public int getNewAssetCount() {
        return newAssetCount;
    }
    public void setNewAssetCount(int newAssetCount) {
        this.newAssetCount = newAssetCount;
    }
    public int getUpdatedAssetCount() {
        return updatedAssetCount;
    }
    public void setUpdatedAssetCount(int updatedAssetCount) {
        this.updatedAssetCount = updatedAssetCount;
    }
    public int getSkippedAssetcount() {
        return skippedAssetcount;
    }
    public void setSkippedAssetcount(int skippedAssetcount) {
        this.skippedAssetcount = skippedAssetcount;
    }
    public int getProxyConfigId() {
        return proxyConfigId;
    }
    public void setProxyConfigId(int proxyConfigId) {
        this.proxyConfigId = proxyConfigId;
    }

    @JsonIgnore
    public boolean isSupportedFileType(String type) {
        if (fileTypes.isEmpty()) {
            return true;
        }

        return fileTypes.contains(type);
    }
}
