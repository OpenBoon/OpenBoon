package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 12/23/15.
 */
public class UserServiceTests extends AbstractTest {

    @Test
    public void createUser() {
        UserBuilder builder = new UserBuilder();
        builder.setUsername("test");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(
                userService.getPermission("group::user"));
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
        assertEquals(2, perms.size());
        assertTrue(userService.hasPermission(user, userService.getPermission("group::user")));
        assertTrue(userService.hasPermission(user, userService.getPermission("user::test")));
        assertFalse(userService.hasPermission(user, userService.getPermission("group::manager")));
    }

    @Test
    public void testDisable() {
        UserBuilder builder = new UserBuilder();
        builder.setUsername("test");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(
                userService.getPermission("group::user"));
        User user = userService.create(builder);

        assertTrue(userService.disable(user));
    }

    @Test
    public void updateUserName() {
        UserBuilder builder = new UserBuilder();
        builder.setUsername("bilbob");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        User user = userService.create(builder);
        assertTrue(userService.hasPermission(user, userService.getPermission("user::bilbob")));

        UserUpdateBuilder update = new UserUpdateBuilder().setUsername("frodo");
        userService.update(user, update);

        assertTrue(userService.hasPermission(user, userService.getPermission("user::frodo")));
        assertTrue(userService.hasPermission(user, "user", "frodo"));
        assertFalse(userService.hasPermission(user, "user", "bilbob"));
    }

    @Test
    public void updatePermissions() {
        UserBuilder builder = new UserBuilder();
        builder.setUsername("bilbob");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(userService.getPermission("group::user"));
        User user = userService.create(builder);

        assertTrue(userService.hasPermission(user, userService.getPermission("group::user")));
        assertFalse(userService.hasPermission(user, userService.getPermission("group::manager")));

        Integer[] permissionIds = { userService.getPermission("group::manager").getId() };
        UserUpdateBuilder update = new UserUpdateBuilder().setPermissionIds(permissionIds);
        userService.update(user, update);

        assertFalse(userService.hasPermission(user, userService.getPermission("group::user")));
        assertTrue(userService.hasPermission(user, userService.getPermission("group::manager")));
    }

    @Test
    public void updateUser() {
        UserBuilder builder = new UserBuilder();
        builder.setUsername("bilbob");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        User user = userService.create(builder);


    }


    @Test
    public void testImmutablePermissions() {
        UserUpdateBuilder builder = new UserUpdateBuilder();
        builder.setPermissionIds(new Integer[]{
                userService.getPermission("group::superuser").getId()});

        User admin = userService.get("admin");
        userService.update(admin, builder);

        logger.info("{}", userService.getPermissions(admin));

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
