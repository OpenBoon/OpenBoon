package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserProfileUpdate;
import com.zorroa.archivist.domain.UserSettings;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Test
    public void testLogin() throws Exception {
        mvc.perform(post("/api/v1/login").session(admin())).andExpect(status().isOk());
    }

    @Test
    public void testSendPasswordRecoveryEmail() throws Exception {
        User user = userService.get("user");
        userService.sendPasswordResetEmail(user);

        SecurityContextHolder.getContext().setAuthentication(null);
        MvcResult result = mvc.perform(post("/api/v1/send-password-reset-email")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com"))))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testResetPassword() throws Exception {
        User user = userService.get("user");
        String token = userService.sendPasswordResetEmail(user);

        SecurityContextHolder.getContext().setAuthentication(null);
        MvcResult result = mvc.perform(post("/api/v1/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Archivist-Recovery-Token", token)
                .content(Json.serialize(ImmutableMap.of(
                        "username", "user", "password", "bob"))))
                .andExpect(status().isOk())
                .andReturn();

        User user2 = Json.deserialize(
                result.getResponse().getContentAsByteArray(), User.class);
        assertEquals(user, user2);
    }

    @Test
    public void testUpdateProfile() throws Exception {

        User user = userService.get("user");

        UserProfileUpdate builder = new UserProfileUpdate();
        builder.setEmail("test@test.com");
        builder.setFirstName("test123");
        builder.setLastName("456test");

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/users/" + user.getId() + "/_profile")
                .session(session)
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        StatusResult<User> sr = Json.deserialize(
                result.getResponse().getContentAsByteArray(), new TypeReference<StatusResult<User>>() {});
        User updated = sr.object;

        assertEquals(user.getId(), updated.getId());
        assertEquals(builder.getEmail(), updated.getEmail());
        assertEquals(builder.getFirstName(), updated.getFirstName());
        assertEquals(builder.getLastName(), updated.getLastName());
    }

    @Test
    public void testUpdateSettings() throws Exception {
        User user = userService.get("user");
        UserSettings settings = new UserSettings();
        settings.setSearch(ImmutableMap.of("foo", "bar"));

        MockHttpSession session = admin();
        MvcResult result = mvc.perform(put("/api/v1/users/" + user.getId() + "/_settings")
                .session(session)
                .content(Json.serialize(settings))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        StatusResult<User> sr = Json.deserialize(
                result.getResponse().getContentAsByteArray(), new TypeReference<StatusResult<User>>() {});
        User updated = sr.object;
        assertEquals(user.getId(), updated.getId());
        assertNotNull(updated.getSettings().getSearch().get("foo"));
        assertEquals("bar", updated.getSettings().getSearch().get("foo"));
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

        userService.setPermissions(user, Lists.newArrayList(userService.getPermission("group::administrator")));
        perms.add(userService.getPermission("group::administrator"));

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
