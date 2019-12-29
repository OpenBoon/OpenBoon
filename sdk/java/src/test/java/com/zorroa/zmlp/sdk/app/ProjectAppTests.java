package com.zorroa.zmlp.sdk.app;

import com.zorroa.zmlp.sdk.ApiKey;
import com.zorroa.zmlp.sdk.Json;
import com.zorroa.zmlp.sdk.ZmlpClient;
import com.zorroa.zmlp.sdk.domain.Project;
import okhttp3.mockwebserver.MockResponse;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

public class ProjectAppTests extends AbstractAppTest {

    ProjectApp projectApp;

    @Before
    public void setup() {
        ApiKey key = new ApiKey(UUID.randomUUID(), "1234");
        projectApp = new ProjectApp(
                new ZmlpClient(key, webServer.url("/").toString()));
    }

    @Test
    public void testGetProject() {
        Map<String, Object> body = new HashMap<>();
        body.put("id", UUID.randomUUID().toString());
        body.put("actorCreated","test@test");
        body.put("actorModified", "test@test");
        body.put("timeCreated", System.currentTimeMillis());
        body.put("timeModified", System.currentTimeMillis());

        webServer.enqueue(new MockResponse().setBody(Json.asJson(body)));
        Project proj = projectApp.getProject(UUID.randomUUID());

        assertEquals(proj.getId().toString(), body.get("id").toString());
        assertEquals(proj.getActorCreated(), body.get("actorCreated"));
        assertEquals(proj.getActorModified(), body.get("actorModified"));
        assertEquals(proj.getTimeCreated(), new Date((Long)body.get("timeCreated")));
        assertEquals(proj.getTimeModified(), new Date((Long)body.get("timeModified")));
    }
}
