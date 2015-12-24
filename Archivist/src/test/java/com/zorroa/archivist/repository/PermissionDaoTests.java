package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.PermissionBuilder;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.domain.UserBuilder;
import com.zorroa.archivist.sdk.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 10/28/15.
 */
public class PermissionDaoTests extends ArchivistApplicationTests {

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    UserService userSerivce;

    Permission perm;

    User user;

    @Before
    public void init() {
        PermissionBuilder b = new PermissionBuilder();
        b.setName("project::avatar");
        b.setDescription("Access to the Avatar project");
        perm = permissionDao.create(b);

        UserBuilder ub = new UserBuilder();
        ub.setUsername("test");
        ub.setPassword("test");
        ub.setFirstName("mr");
        ub.setLastName("test");
        ub.setEmail("test@zorroa.com");

        user = userService.create(ub);
    }

    @Test
    public void testCreate() {
        PermissionBuilder b = new PermissionBuilder();
        b.setName("group::test");
        b.setDescription("test");

        Permission p = permissionDao.create(b);
        assertEquals(p.getName(), b.getName());
        assertEquals(p.getDescription(), b.getDescription());
    }

    @Test
    public void testGet() {
        Permission p = permissionDao.get(perm.getId());
        assertEquals(perm.getName(), p.getName());
        assertEquals(perm.getDescription(), p.getDescription());
    }

    @Test
    public void testGetAll() {
        List<Permission> perms = permissionDao.getAll();
        assertTrue(perms.size() > 0);
    }

    @Test
    public void testGetAllByType() {
        List<Permission> perms = permissionDao.getAll("user");
        /*
         * There are 3 active users in this test: admin, user, and test.
         */
        assertEquals(3, perms.size());
    }

    @Test
    public void testGetAllByIds() {
        List<Permission> perms1 = permissionDao.getAll();
        List<Permission> perms2 = permissionDao.getAll(new Integer[] {
            perms1.get(0).getId(), perms1.get(1).getId()
        });
        assertEquals(2, perms2.size());
        assertTrue(perms2.contains(perms1.get(0)));
        assertTrue(perms2.contains(perms1.get(1)));
    }


    @Test
    public void testSetOnUser() {
        permissionDao.setOnUser(user, Lists.newArrayList(permissionDao.get("group::manager")));
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(permissionDao.get("group::manager")));
    }

    @Test
    public void testSetOnUserVargs() {
        permissionDao.setOnUser(user, permissionDao.get("group::manager"));
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(permissionDao.get("group::manager")));
    }

    @Test
    public void testAssignPermission() {
        assertTrue(permissionDao.assign(user, permissionDao.get("group::manager"), false));
        assertFalse(permissionDao.assign(user, permissionDao.get("group::manager"), false));
    }

    @Test
    public void testDelete() {
        /*
         * Internally managed permissions cannot be deleted in this way.
         */
        assertFalse(permissionDao.delete(permissionDao.get("group::manager")));
        assertTrue(permissionDao.delete(permissionDao.get("project::avatar")));
    }

    @Test
    public void testDeleteByUser() {
        /*
         * User permissions are deleted by user.
         */
        assertTrue(permissionDao.delete(user));
        assertFalse(permissionDao.delete(user));
    }
}
