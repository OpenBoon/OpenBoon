package com.zorroa.zmlp.sdk.app;

import com.google.common.collect.Lists;
import com.zorroa.zmlp.sdk.ApiKey;
import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.*;
import com.zorroa.zmlp.sdk.domain.project.Project;
import com.zorroa.zmlp.sdk.domain.project.ProjectFilter;
import com.zorroa.zmlp.sdk.domain.project.ProjectSpec;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ProjectAppTests extends AbstractAppTests {

    ProjectApp projectApp;

    @Before
    public void setup() {
        ApiKey key = new ApiKey("abcd", "1234");
        projectApp = new ProjectApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    @Test
    public void testGetProject() {
        Map<String, Object> body = getProjectBody();
        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));
        UUID id = UUID.randomUUID();
        Project proj = projectApp.getProject(id);

        assertEquals(proj.getId().toString(), body.get("id").toString());
        assertEquals(proj.getActorCreated(), body.get("actorCreated"));
        assertEquals(proj.getActorModified(), body.get("actorModified"));
        assertEquals(proj.getTimeCreated(), new Date((Long) body.get("timeCreated")));
        assertEquals(proj.getTimeModified(), new Date((Long) body.get("timeModified")));
    }

    @Test
    public void testCreateProject() {
        Map<String, Object> body = getProjectBody();

        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        ProjectSpec unittest = new ProjectSpec().withName("unittest");
        Project proj = projectApp.createProject(unittest);

        assertEquals(proj.getId().toString(), body.get("id").toString());
        assertEquals(proj.getActorCreated(), body.get("actorCreated"));
        assertEquals(proj.getActorModified(), body.get("actorModified"));
        assertEquals(proj.getTimeCreated(), new Date((Long) body.get("timeCreated")));
        assertEquals(proj.getTimeModified(), new Date((Long) body.get("timeModified")));
    }

    @Test
    public void testFindProject() {
        Map<String, Object> body = getProjectBody();

        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));

        ProjectFilter filter = new ProjectFilter()
                .withIds(Lists.newArrayList(UUID.randomUUID()));
        Project proj = projectApp.findProject(filter);

        assertEquals(proj.getId().toString(), body.get("id").toString());
        assertEquals(proj.getActorCreated(), body.get("actorCreated"));
        assertEquals(proj.getActorModified(), body.get("actorModified"));
        assertEquals(proj.getTimeCreated(), new Date((Long) body.get("timeCreated")));
        assertEquals(proj.getTimeModified(), new Date((Long) body.get("timeModified")));
    }

    @Test
    public void testSearchProjects() {
        Map<String, Object> responseProject = getProjectBody();
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("list", Lists.newArrayList(responseProject));
        responseBody.put("page", new Page().setSize(10));

        webServer.enqueue(new MockResponse().setBody(Json.asJson(responseBody)));
        ProjectFilter filter = new ProjectFilter()
                .withIds(Lists.newArrayList(UUID.randomUUID()));
        PagedList<Project> projects = projectApp.searchProjects(filter);

        assertEquals(1, projects.size());
        Project proj = projects.getList().get(0);

        assertEquals(proj.getId().toString(), responseProject.get("id").toString());
        assertEquals(proj.getActorCreated(), responseProject.get("actorCreated"));
        assertEquals(proj.getActorModified(), responseProject.get("actorModified"));
        assertEquals(proj.getTimeCreated(), new Date((Long) responseProject.get("timeCreated")));
        assertEquals(proj.getTimeModified(), new Date((Long) responseProject.get("timeModified")));
    }

    private Map<String, Object> getProjectBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("actorCreated", "test@test");
        body.put("actorModified", "test@test");
        body.put("timeCreated", System.currentTimeMillis());
        body.put("timeModified", System.currentTimeMillis());
        return body;
    }
}
