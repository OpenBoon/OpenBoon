package com.zorroa.zmlp.client.domain.project;

import java.util.Date;
import java.util.UUID;

/**
 * Projects represent unique groups of resources provided by ZMLP.
 */
public class Project {

    /**
     * The Unique ID of the project.
     */
    UUID id;

    /**
     * The name of the Project
     */
    String name;

    /**
     * The time the Project was created.
     */
    Date timeCreated;

    /**
     * The last time the Project was modified.
     */
    Date timeModified;

    /**
     * The actor which created this Project
     */
    String actorCreated;

    /**
     * The actor that last made the last modification the project.
     */
    String actorModified;

    Boolean enabled;

    public Project() {
    }

    public UUID getId() {
        return id;
    }

    public Project setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Project setName(String name) {
        this.name = name;
        return this;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public Project setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public Date getTimeModified() {
        return timeModified;
    }

    public Project setTimeModified(Date timeModified) {
        this.timeModified = timeModified;
        return this;
    }

    public String getActorCreated() {
        return actorCreated;
    }

    public Project setActorCreated(String actorCreated) {
        this.actorCreated = actorCreated;
        return this;
    }

    public String getActorModified() {
        return actorModified;
    }

    public Project setActorModified(String actorModified) {
        this.actorModified = actorModified;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Project setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }
}
