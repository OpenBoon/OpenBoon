package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by chambers on 8/16/16.
 */
public class Plugin {

    private int id;
    private String name;
    private String description;
    private String version;
    private String publisher;
    private String language;

    public int getId() {
        return id;
    }

    public Plugin setId(int id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Plugin setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Plugin setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public Plugin setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getPublisher() {
        return publisher;
    }

    public Plugin setPublisher(String publisher) {
        this.publisher = publisher;
        return this;
    }

    public String getLanguage() {
        return language;
    }

    public Plugin setLanguage(String language) {
        this.language = language;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Plugin plugin = (Plugin) o;
        return getId() == plugin.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("version", version)
                .toString();
    }
}
