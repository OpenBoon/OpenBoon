package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 10/27/15.
 */
public class PermissionBuilder {

    private String name;
    private String description;

    public String getDescription() {
        return description;
    }

    public PermissionBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getName() {
        return name;
    }

    public PermissionBuilder setName(String name) {
        this.name = name;
        return this;
    }
}
