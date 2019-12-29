package com.zorroa.zmlp.sdk.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    UUID projectId;

    public ProjectSpec() { }

    public ProjectSpec(String name, UUID projectId) {
        this.name = name;
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public void setProjectId(UUID projectId) {
        this.projectId = projectId;
    }
}

