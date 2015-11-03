package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by chambers on 10/27/15.
 */
public class Permission {

    private int id;
    private String name;
    private String description;

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

    @Override
    public int hashCode() {
        return Objects.hash(id, 829);
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {  return false; }
        if (getClass() != other.getClass()) { return false; }
        return Objects.equals(id, ((Permission)other).getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("id", id)
                .add("name", name)
                .add("description", description)
                .toString();
    }
}
