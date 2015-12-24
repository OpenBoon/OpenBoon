package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.domain.UserBuilder;
import com.zorroa.archivist.sdk.service.FolderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 12/23/15.
 */
public class UserServiceTests extends ArchivistApplicationTests {

    @Autowired
    FolderService folderService;

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
        authenticate(user.getUsername(), "123password");

        /*
         * Get the user's personal folder.
         */
        Folder folder = folderService.get("/users/test");
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



}
