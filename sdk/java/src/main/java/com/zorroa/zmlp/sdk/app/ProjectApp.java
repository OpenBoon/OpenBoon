package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Project;
import com.zorroa.zmlp.sdk.domain.ProjectFilter;
import com.zorroa.zmlp.sdk.domain.ProjectSpec;

import java.io.IOException;
import java.util.*;

public class ProjectApp {

    private ZmlpClient client;

    public ProjectApp(ZmlpClient client) {
        this.client = client
    }

    /**
     * @param spec Project Params
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Project createProject(ProjectSpec spec) throws IOException, InterruptedException {

        Map post = this.client("/api/v1/projects", spec.toMap());
        return new Project(post);

    }

    /**
     * @param params Project Filter parameters
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Project searchProject(ProjectFilter params) throws IOException, InterruptedException {
        Map response = this.post("/api/v1/projects/_findOne", params.toMap());
        return new Project(response);

    }

    /**
     * @param uuid Project Unique Key
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Boolean deleteProject(UUID uuid) throws IOException, InterruptedException {
        Map response = this.delete("/api/v1/projects", uuid);
        return (Boolean) response.get("success");
    }

    /**
     *
     * @param projectFilter Project Filter
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public List<Project> getAllProjects(ProjectFilter projectFilter) throws IOException, InterruptedException {
        Map response = this.post("/api/v1/projects/_search", projectFilter.toMap());
        List<Project> projects = new ArrayList();
        Optional.ofNullable(response.get("list"))
                .ifPresent((mapList) -> {
                            for (Map p : (List<Map>) mapList)
                                projects.add(new Project(p));
                        }
                );

        return projects;
    }
}
