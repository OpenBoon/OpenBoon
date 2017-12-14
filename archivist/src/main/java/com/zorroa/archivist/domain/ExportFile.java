package com.zorroa.archivist.domain;

import java.util.Objects;

public class ExportFile {

    private long id;
    private long jobId;
    private String name;
    private String mimeType;
    private long size;
    private long timeCreated;

    public long getId() {
        return id;
    }

    public ExportFile setId(long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ExportFile setName(String name) {
        this.name = name;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ExportFile setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public long getSize() {
        return size;
    }

    public ExportFile setSize(long size) {
        this.size = size;
        return this;
    }

    public long getJobId() {
        return jobId;
    }

    public ExportFile setJobId(long jobId) {
        this.jobId = jobId;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public ExportFile setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportFile that = (ExportFile) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getId());
    }
}
