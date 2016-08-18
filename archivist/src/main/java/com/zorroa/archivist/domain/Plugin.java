package com.zorroa.archivist.domain;

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
}
