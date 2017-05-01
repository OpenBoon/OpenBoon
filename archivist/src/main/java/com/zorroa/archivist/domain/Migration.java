package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * A Migration is some aspect of the Archivist can change from one version to the next.
 */
public class Migration {

    private int id;
    private MigrationType type;
    private String name;
    private String path;
    private int version;
    private int patch;

    public int getId() {
        return id;
    }

    public Migration setId(int id) {
        this.id = id;
        return this;
    }

    public MigrationType getType() {
        return type;
    }

    public Migration setType(MigrationType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Migration setName(String name) {
        this.name = name;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Migration setPath(String path) {
        this.path = path;
        return this;
    }

    public int getVersion() {
        return version;
    }

    public Migration setVersion(int version) {
        this.version = version;
        return this;
    }

    public int getPatch() {
        return patch;
    }

    public Migration setPatch(int patch) {
        this.patch = patch;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("name", name)
                .add("version", version)
                .add("patch", patch)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Migration migration = (Migration) o;
        return getId() == migration.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
