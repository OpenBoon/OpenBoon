package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.domain.*;
import com.zorroa.security.Groups;
import com.zorroa.sdk.util.Json;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
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
        emailService.sendPasswordResetEmail(user);

        SecurityContextHolder.getContext().setAuthentication(null);
        MvcResult result = mvc.perform(post("/api/v1/send-password-reset-email")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com"))))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testSendOnboardEmail() throws Exception {
        User user = userService.get("user");

        SecurityContextHolder.getContext().setAuthentication(null);
        MvcResult result = mvc.perform(post("/api/v1/send-onboard-email")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com"))))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testResetPassword() throws Exception {
        User user = userService.get("user");
        PasswordResetToken token = emailService.sendPasswordResetEmail(user);
        assertTrue(token.isEmailSent());

        SecurityContextHolder.getContext().setAuthentication(null);
        MvcResult result = mvc.perform(post("/api/v1/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-Archivist-Recovery-Token", token.getToken())
                .content(Json.serialize(ImmutableMap.of(
                        "username", "user", "password", "Bilb0Baggins"))))
                .andExpect(status().isOk())
                .andReturn();

        User user2 = Json.deserialize(
                result.getResponse().getContentAsByteArray(), User.class);
        assertEquals(user.getId(), user2.getId());
    }

    @Test
    public void testUpdateProfile() throws Exception {

        User user = userService.get("user");

        UserProfileUpdate builder = new UserProfileUpdate();
        builder.setUsername("foo");
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
        assertEquals(builder.getUsername(), updated.getUsername());
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
    public void testEnableDisable() throws Exception {
        User user = userService.get("user");
        MockHttpSession session = admin();
        mvc.perform(put("/api/v1/users/" + user.getId() + "/_enabled")
                .session(session)
                .content(Json.serialize(ImmutableMap.of("enabled", false)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        user = userService.get("user");
        assertFalse(user.getEnabled());

        mvc.perform(put("/api/v1/users/" + user.getId() + "/_enabled")
                .session(session)
                .content(Json.serialize(ImmutableMap.of("enabled", true)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        user = userService.get("user");
        assertTrue(user.getEnabled());
    }


    @Test
    public void testDisableSelf() throws Exception {
        MockHttpSession session = admin();
        mvc.perform(put("/api/v1/users/1/_enabled")
                .content(Json.serialize(ImmutableMap.of("enabled", false)))
                .session(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void testSetPermissions() throws Exception {

        User user = userService.get("user");
        List<UUID> perms = permissionService.getPermissions().stream().map(
                p->p.getId()).collect(Collectors.toList());

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

        userService.setPermissions(user, Lists.newArrayList(permissionService.getPermission(Groups.ADMIN)));
        perms.add(permissionService.getPermission(Groups.ADMIN));

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
