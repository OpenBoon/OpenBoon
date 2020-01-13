package com.zorroa.zmlp.sdk.domain.project;

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

    public Project() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Date getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(Date timeModified) {
        this.timeModified = timeModified;
    }

    public String getActorCreated() {
        return actorCreated;
    }

    public void setActorCreated(String actorCreated) {
        this.actorCreated = actorCreated;
    }

    public String getActorModified() {
        return actorModified;
    }

    public void setActorModified(String actorModified) {
        this.actorModified = actorModified;
    }
}
