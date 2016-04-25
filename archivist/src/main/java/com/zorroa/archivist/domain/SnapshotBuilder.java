package com.zorroa.archivist.domain;

public class SnapshotBuilder {

    private String name;

    public SnapshotBuilder() { }

    public SnapshotBuilder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
