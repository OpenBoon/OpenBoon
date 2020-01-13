package com.zorroa.zmlp.client.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.zmlp.client.ZmlpClient;
import com.zorroa.zmlp.client.domain.PagedList;
import com.zorroa.zmlp.client.domain.project.Project;
import com.zorroa.zmlp.client.domain.project.ProjectFilter;
import com.zorroa.zmlp.client.domain.project.ProjectSpec;

import java.util.UUID;

public class ProjectApp {

    private ZmlpClient client;

    public ProjectApp(ZmlpClient client) {
        this.client = client;
    }

    /**
     * Create a new project.
     *
     * @param spec A ProjectSpec instance.
     * @return The created Project
     */
    public Project createProject(ProjectSpec spec) {
        return client.post("/api/v1/projects", spec, Project.class);
    }

    /**
     * Find a Project with its unique Id.
     *
     * @param id
     * @return The found Project
     */
    public Project getProject(UUID id) {
        return client.get("/api/v1/projects/" + id.toString(), null, Project.class);
    }

    /**
     * Find a Project with the given filter.  Filter must limit result count to 1.
     *
     * @param filter
     * @return The found Project
     */
    public Project findProject(ProjectFilter filter) {
        return client.post("/api/v1/projects/_findOne", filter, Project.class);
    }

    /**
     * @param filter
     * @return
     */
    public PagedList<Project> searchProjects(ProjectFilter filter) {
        return client.post("/api/v1/projects/_search", filter, new TypeReference<PagedList<Project>>() {
        });
    }
}
