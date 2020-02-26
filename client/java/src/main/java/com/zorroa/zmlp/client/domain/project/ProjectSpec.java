package com.zorroa.zmlp.client.domain.project;

import java.util.UUID;

/**
 * All properties necessary to create a Project.
 */
public class ProjectSpec {

    /**
     * A unique name of the project.
     */
    String name;

    /**
     * An optional unique ID for the project.
     */

    UUID id;

    public ProjectSpec() {
    }

    public ProjectSpec(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ProjectSpec setName(String name) {
        this.name = name;
        return this;
    }

    public UUID getId() {
        return id;
    }

    public ProjectSpec setId(UUID id) {
        this.id = id;
        return this;
    }
}

