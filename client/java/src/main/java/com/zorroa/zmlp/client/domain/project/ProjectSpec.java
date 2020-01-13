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

    UUID projectId;

    public ProjectSpec() {
    }

    public ProjectSpec(String name) {
        this.name = name;
    }

    public ProjectSpec withName(String name){
        this.name = name;
        return this;
    }

    public ProjectSpec withProjectId(UUID projectId){
        this.projectId = projectId;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProjectSpec setName(String name) {
        this.name = name;
        return this;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public ProjectSpec setProjectId(UUID projectId) {
        this.projectId = projectId;
        return this;
    }
}

