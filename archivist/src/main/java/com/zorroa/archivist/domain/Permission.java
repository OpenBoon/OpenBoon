package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by chambers on 10/27/15.
 */
public class Permission implements Serializable {

    public static final String JOIN = "::";

    private int id;
    private String name;
    private String type;
    private String description;
    private boolean immutable;

    public String getFullName() {
        return type.concat(JOIN).concat(name);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public Permission setImmutable(boolean immutable) {
        this.immutable = immutable;
        return this;
    }

    @JsonIgnore
    public String getAuthority() {
        return new StringBuilder(64).append(type).append("::").append(name).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, 829);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {  return false; }
        try {
            return Objects.equals(id, ((Permission) other).getId());
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("name", name)
                .add("type", type)
                .add("description", description)
                .toString();
    }
}
