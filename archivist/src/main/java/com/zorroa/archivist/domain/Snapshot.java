package com.zorroa.archivist.domain;

public class Snapshot {

    private String name;
    private long startTime;
    private long endTime;
    private SnapshotState state;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public SnapshotState getState() {
        return state;
    }

    public void setState(SnapshotState state) {
        this.state = state;
    }
}
