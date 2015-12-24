package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 10/27/15.
 */
public class PermissionBuilder {

    private String name;
    private String description;

    /**
     * Immutable permissions are managed by the archivist and cannot be manually
     * removed via the API.
     */
    private final boolean immutable;

    public PermissionBuilder() {
        this.immutable = false;
    }

    public PermissionBuilder(String type, String name, boolean immutable) {
        setName(String.format("%s::%s", type, name));
        this.immutable = false;
    }

    public PermissionBuilder(String name) {
        setName(name);
        this.immutable = false;
    }

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

    @JsonIgnore
    public String getType() {
        return name.split("::")[0];
    }

    public PermissionBuilder setName(String name) {
        if (!name.contains("::") || name.indexOf("::") != name.lastIndexOf("::")) {
            throw new IllegalArgumentException("Permissions must be in the format of 'namespace::group'");
        }
        this.name = name;
        return this;
    }

    @JsonIgnore
    public boolean isImmutable() {
        return immutable;
    }
}
