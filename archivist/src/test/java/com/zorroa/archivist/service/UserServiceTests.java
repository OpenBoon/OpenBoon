package com.zorroa.archivist.service;

import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.sdk.domain.Permission;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 12/23/15.
 */
public class UserServiceTests extends AbstractTest {

    @Test
    public void createUser() {
        UserSpec builder = new UserSpec();
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
        UserSpec builder = new UserSpec();
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
    public void updatePermissions() {
        UserSpec builder = new UserSpec();
        builder.setUsername("bilbob");
        builder.setPassword("123password");
        builder.setEmail("test@test.com");
        builder.setFirstName("Bilbo");
        builder.setLastName("Baggings");
        builder.setPermissions(userService.getPermission("group::user"));
        User user = userService.create(builder);

        assertTrue(userService.hasPermission(user, userService.getPermission("group::user")));
        assertFalse(userService.hasPermission(user, userService.getPermission("group::manager")));

        userService.setPermissions(user, userService.getPermission("group::manager"));

        assertFalse(userService.hasPermission(user, userService.getPermission("group::user")));
        assertTrue(userService.hasPermission(user, userService.getPermission("group::manager")));
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

        UserUpdate update = new UserUpdate();
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
                userService.getPermission("group::superuser").getId()});

        User admin = userService.get("admin");
        userService.setPermissions(admin, userService.getPermission("group::superuser"));

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
