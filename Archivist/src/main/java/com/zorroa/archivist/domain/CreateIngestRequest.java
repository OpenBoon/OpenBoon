package com.zorroa.archivist.domain;

import java.util.List;
import java.util.Set;

public class CreateIngestRequest {

    /**
     * An ingest request may include multiple directories to be scanned, direct paths to individual files,
     * or a mixture of the two.
     */
    private List<String> paths;

    /**
     * Extention filers for the target file types. Ex: jpg, png, etc.
     */
    private Set<String> fileTypes;

    /**
     * The time at which the record was created.
     */
    private long timeCreated = System.currentTimeMillis();

    /**
     * The default state of the ingest
     */
    private final IngestState state = IngestState.WAITING;

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
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

    public IngestState getState() {
        return state;
    }
}
