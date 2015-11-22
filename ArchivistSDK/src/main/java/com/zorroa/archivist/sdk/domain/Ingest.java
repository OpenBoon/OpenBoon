package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;
import java.util.Set;

public class Ingest {

    private long id;
    private int pipelineId;
    private IngestState state;
    private String path;
    private Set<String> fileTypes;
    private long timeCreated;
    private int userCreated;
    private long timeModified;
    private int userModified;
    private long timeStopped;
    private long timeStarted;
    private int createdCount;
    private int updatedCount;
    private int errorCount;
    private boolean updateOnExist;
    private int assetWorkerThreads;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", getId())
                .add("state", getState())
                .add("path", getPath())
                .toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        try {
            return id == ((Ingest)other).getId();
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

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
    public int getUserCreated() {
        return userCreated;
    }
    public void setUserCreated(int userCreated) {
        this.userCreated = userCreated;
    }
    public long getTimeModified() {
        return timeModified;
    }
    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }
    public int getUserModified() {
        return userModified;
    }
    public void setUserModified(int userModified) {
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
    public int getUpdatedCount() {
        return updatedCount;
    }
    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public boolean isSupportedFileType(String type) {
        if (fileTypes.isEmpty()) {
            return true;
        }

        return fileTypes.contains(type);
    }
    public int getCreatedCount() {
        return createdCount;
    }
    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }
    public int getErrorCount() {
        return errorCount;
    }
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
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
