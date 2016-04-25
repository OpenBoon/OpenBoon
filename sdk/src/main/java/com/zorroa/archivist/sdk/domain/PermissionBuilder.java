package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by chambers on 10/27/15.
 */
public class PermissionBuilder {

    private String name;
    private String type;
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
        this.name = name;
        this.type = type;
        this.immutable = false;
    }

    public PermissionBuilder(String type, String name) {
        setType(type);
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

    public String getType() {
        return type; }

    public PermissionBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public PermissionBuilder setType(String type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public boolean isImmutable() {
        return immutable;
    }
}

