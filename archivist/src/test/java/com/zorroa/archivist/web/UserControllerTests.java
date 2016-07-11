package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.archivist.web.api.UserController;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.NestedServletException;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
public class UserControllerTests extends MockMvcTest {

    @Autowired
    UserController userController;

    @Test
    public void testLogin() throws Exception {
        mvc.perform(post("/api/v1/login").session(admin())).andExpect(status().isOk());
    }

    @Test
    public void testUpdate() throws Exception {

        User user = userService.get("user");

        UserUpdate builder = new UserUpdate();
        builder.setEmail("test@test.com");
        builder.setFirstName("test123");
        builder.setLastName("456test");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/users/" + user.getId())
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        User updated = Json.Mapper.readValue(result.getResponse().getContentAsString(), User.class);
        assertEquals(user.getId(), updated.getId());
        assertEquals(builder.getEmail(), updated.getEmail());
        assertEquals(builder.getFirstName(), updated.getFirstName());
        assertEquals(builder.getLastName(), updated.getLastName());
    }

    @Test
    public void testDisable() throws Exception {
        User user = userService.get("user");
        MockHttpSession session = admin();
        mvc.perform(delete("/api/v1/users/" + user.getId())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        user = userService.get("user");
        assertFalse(user.getEnabled());
    }

    @Test(expected = NestedServletException.class)
    public void testDisableSelf() throws Exception {
        MockHttpSession session = admin();
        mvc.perform(delete("/api/v1/users/1")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
    }

    @Test
    public void testSetPermissions() throws Exception {

        User user = userService.get("user");
        List<Integer> perms = userService.getPermissions().stream().mapToInt(
                p->p.getId()).boxed().collect(Collectors.toList());

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/users/" + user.getId() + "/permissions")
                .session(session)
                .content(Json.serialize(perms))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Permission> response = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Permission>>() {});
        assertEquals(response, userService.getPermissions(user));
    }

    @Test
    public void testGetPermissions() throws Exception {

        User user = userService.get("user");
        List<Permission> perms = userService.getPermissions(user);
        assertTrue(perms.size() > 0);

        userService.setPermissions(user, userService.getPermission("group::superuser"));
        perms.add(userService.getPermission("group::superuser"));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(get("/api/v1/users/" + user.getId() + "/permissions")
                .session(session)
                .content(Json.serialize(perms))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<Permission> response = Json.Mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<Permission>>() {});
        assertEquals(response, userService.getPermissions(user));
    }
}
