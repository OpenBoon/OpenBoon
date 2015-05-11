package com.zorroa.archivist.domain;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Ingest {

    private String id;
    private List<String> paths;
    private Set<String> fileTypes;
    private IngestState state;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
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
    public IngestState getState() {
        return state;
    }
    public void setState(IngestState state) {
        this.state = state;
    }
}
