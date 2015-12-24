package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

/**
 * Created by chambers on 11/1/15.
 */
public class Export {

    private int id;
    private String note;
    private long timeCreated;
    private int userCreated;
    private ExportState state;
    private long totalFileSize;
    private int assetCount;

    private AssetSearch search;
    private ExportOptions options;

    private long timeStarted;
    private long timeStopped;
    private int executeCount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public ExportState getState() {
        return state;
    }

    public void setState(ExportState state) {
        this.state = state;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public void setSearch(AssetSearch search) {
        this.search = search;
    }

    public ExportOptions getOptions() {
        return options;
    }

    public void setOptions(ExportOptions options) {
        this.options = options;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getTotalFileSize() {
        return totalFileSize;
    }

    public void setTotalFileSize(long totalFileSize) {
        this.totalFileSize = totalFileSize;
    }

    public int getAssetCount() {
        return assetCount;
    }

    public void setAssetCount(int assetCount) {
        this.assetCount = assetCount;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    public void setTimeStarted(long timeStarted) {
        this.timeStarted = timeStarted;
    }

    public long getTimeStopped() {
        return timeStopped;
    }

    public void setTimeStopped(long timeStopped) {
        this.timeStopped = timeStopped;
    }

    public int getExecuteCount() {
        return executeCount;
    }

    public void setExecuteCount(int executeCount) {
        this.executeCount = executeCount;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("state", state.toString())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Export export = (Export) o;
        return id == export.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
