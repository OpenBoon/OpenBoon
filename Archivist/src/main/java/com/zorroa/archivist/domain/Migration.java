package com.zorroa.archivist.domain;

/**
 * A Migration is some aspect of the Archivist can change from one version to the next.
 */
public class Migration {

    private int id;
    private MigrationType type;
    private String name;
    private String path;
    private int version;

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
}
