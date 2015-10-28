package com.zorroa.archivist.domain;

/**
 * Created by chambers on 10/27/15.
 */
public class PermissionBuilder {

    private String name;
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
