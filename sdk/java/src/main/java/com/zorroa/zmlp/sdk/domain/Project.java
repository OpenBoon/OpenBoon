package com.zorroa.zmlp.sdk.domain;

import java.util.Map;
import java.util.UUID;

public class Project {

    /**
     * The Unique ID of the project.
     */
    private UUID id;

    /**
     * The name of the Project
     */
    private String name;

    /**
     * The time the Project was created.
     */
    private Long timeCreated;

    /**
     * The last time the Project was modified.
     */
    private Long timeModified;

    /**
     * The actor which created this Project
     */
    private String actorCreated;

    /**
     * The actor that last made the last modification the project.
     */
    private String actorModified;

    public Project() { }

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

    public Long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Long getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(Long timeModified) {
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
