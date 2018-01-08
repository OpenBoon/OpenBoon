package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionSpec;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by chambers on 10/28/15.
 */
public class PermissionContollerTests extends MockMvcTest {

    @Test
    public void testCreate() throws Exception {
        PermissionSpec b = new PermissionSpec("project", "sw");
        b.setDescription("Star Wars crew members");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(post("/api/v1/permissions")
                .session(session)
                .content(Json.serialize(b))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Permission p = Json.deserialize(result.getResponse().getContentAsByteArray(), Permission.class);
        assertEquals(b.getDescription(), p.getDescription());
        assertEquals(b.getName(), p.getName());
    }

    @Test
    public void testGetAll() throws Exception {

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/permissions")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Permission> perms1 = Json.Mapper.readValue(result.getResponse().getContentAsByteArray(),
                new TypeReference<List<Permission>>() {
                });
        List<Permission> perms2 = permissionService.getPermissions();
        assertEquals(perms1, perms2);
    }

    @Test
    public void testGet() throws Exception {

        PermissionSpec b = new PermissionSpec("project", "sw");
        b.setName("project::sw");
        b.setDescription("Star Wars crew members");
        Permission perm = permissionService.createPermission(b);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/permissions/" + perm.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Permission perm1 = Json.deserialize(result.getResponse().getContentAsByteArray(), Permission.class);
        assertEquals(perm, perm1);
    }

    @Test
    public void testFoo() throws Exception {

        PermissionSpec b = new PermissionSpec("project", "sw");
        b.setDescription("Star Wars crew members");
        Permission perm = permissionService.createPermission(b);

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/permissions/" + perm.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Permission perm1 = Json.deserialize(result.getResponse().getContentAsByteArray(), Permission.class);
        assertEquals(perm, perm1);
    }
}
