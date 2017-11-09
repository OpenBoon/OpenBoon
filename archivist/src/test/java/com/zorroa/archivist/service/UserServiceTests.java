package com.zorroa.archivist.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.client.exception.DuplicateElementException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 12/23/15.
 */
public class UserServiceTests extends AbstractTest {

    User testUser;

    @Before
    public void init() {
        UserSpec builder = new UserSpec();
        builder.setUsername("billybob");
        builder.setPassword("123password!");
        builder.setEmail("testing@testing123.com");
        builder.setFirstName("BillyBob");
        builder.setLastName("Rodriquez");
        testUser = userService.create(builder);
    }

    @Test
    public void createUser() {
        UserSpec builder = new UserSpec();
        builder.setUsername("test");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(
                userService.getPermission("group::manager"));
        User user = userService.create(builder);

        /*
         * Re-authenticate this test as the user we just made.  Otherwise we can't
         * access the user folder.
         */
        authenticate(user.getUsername());

        /*
         * Get the user's personal folder.
         */
        Folder folder = folderService.get("/Users/test");
        assertEquals(folder.getName(), user.getUsername());

        /*
         * Get the permissions for the user.  This should contain the user's
         * personal permission and the user group
         */
        List<Permission> perms = userService.getPermissions(user);
        assertEquals(3, perms.size());
        assertTrue(userService.hasPermission(user, userService.getPermission("group::manager")));
        assertTrue(userService.hasPermission(user, userService.getPermission("user::test")));
        assertFalse(userService.hasPermission(user, userService.getPermission("group::developer")));
    }

    @Test(expected=DuplicateElementException.class)
    public void createDuplicateUser() {
        UserSpec builder = new UserSpec();
        builder.setUsername("test");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(
                userService.getPermission("group::manager"));
        userService.create(builder);
        userService.create(builder);
    }

    @Test
    public void createUserWithPresets() {
        UserPreset presets = userService.createUserPreset(new UserPresetSpec()
                .setName("defaults")
                .setPermissionIds(Lists.newArrayList(userService.getPermission("group::manager").getId()))
                .setSettings(new UserSettings().setSearch(ImmutableMap.of("foo", "bar"))));

        UserSpec builder = new UserSpec();
        builder.setUsername("bilbo");
        builder.setPassword("123password");
        builder.setEmail("bilbo@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setUserPresetId(presets.getPresetId());

        User user = userService.create(builder);
        assertTrue(userService.hasPermission(user, userService.getPermission("group::manager")));

        UserSettings settings = user.getSettings();
        assertEquals("bar", settings.getSearch().get("foo"));
    }

    @Test
    public void testSetEnabled() {
        UserSpec builder = new UserSpec();
        builder.setUsername("test");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(
                userService.getPermission("group::manager"));
        User user = userService.create(builder);

        assertTrue(userService.setEnabled(user, false));
    }

    @Test
    public void updatePermissions() {
        UserSpec builder = new UserSpec();
        builder.setUsername("bilbob");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(userService.getPermission("group::manager"));
        User user = userService.create(builder);

        Permission dev = userService.getPermission("group::developer");
        assertTrue(userService.hasPermission(user, userService.getPermission("group::manager")));
        assertFalse(userService.hasPermission(user, dev));

        userService.setPermissions(user, Lists.newArrayList(dev));

        assertFalse(userService.hasPermission(user, userService.getPermission("group::manager")));
        assertTrue(userService.hasPermission(user, dev));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updatePasswordFailureTooShort() {
        try {
            userService.resetPassword(testUser, "nogood");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("or more characters"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void updatePasswordFailureNoCaps() {
        try {
            userService.resetPassword(testUser, "nogood");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("upper case"));
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void updatePasswordFailureNoNumbers() {
        try {
            userService.resetPassword(testUser, "nogood");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("least 1 number."));
            throw e;
        }
    }

    @Test
    public void updatePassword() {
        userService.resetPassword(testUser, "DogCatched1");
        userService.checkPassword(testUser.getUsername(), "DogCatched1");
    }

    @Test
    public void updateUser() {
        UserSpec builder = new UserSpec();
        builder.setUsername("bilbob");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        User user = userService.create(builder);

        UserProfileUpdate update = new UserProfileUpdate();
        update.setFirstName("foo");
        update.setLastName("bar");
        update.setEmail("test@test.com");

        assertTrue(userService.update(user, update));
        User updated = userService.get(user.getId());
        assertEquals(update.getEmail(), updated.getEmail());
        assertEquals(update.getFirstName(), updated.getFirstName());
        assertEquals(update.getLastName(), updated.getLastName());
    }

    @Test
    public void testImmutablePermissions() {
        UserSpec builder = new UserSpec();
        builder.setPermissionIds(new Integer[]{
                userService.getPermission("group::administrator").getId()});

        Permission sup = userService.getPermission("group::administrator");

        User admin = userService.get("admin");
        userService.setPermissions(admin, Lists.newArrayList(sup));

        for (Permission p: userService.getPermissions(admin)) {
            logger.info("{}", p);
            if (p.getName().equals("admin") && p.getType().equals("user")) {
                // We found the user permission, all good
                return;
            }
        }
        throw new RuntimeException("The admin user was missing the user::admin permission");
    }

}
