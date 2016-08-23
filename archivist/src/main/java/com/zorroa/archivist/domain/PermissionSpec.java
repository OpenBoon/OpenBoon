package com.zorroa.archivist.domain;

/**
 * Created by chambers on 10/27/15.
 */
public class PermissionSpec {

    private String name;
    private String type;
    private String description;

    public PermissionSpec() {}

    public PermissionSpec(String type, String name) {
        this.name = name;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public PermissionSpec setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type; }

    public PermissionSpec setName(String name) {
        this.name = name;
        return this;
    }

    public PermissionSpec setType(String type) {
        this.type = type;
        return this;
    }
}

