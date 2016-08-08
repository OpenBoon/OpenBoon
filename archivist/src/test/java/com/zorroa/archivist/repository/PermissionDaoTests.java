package com.zorroa.archivist.repository;

import com.google.common.collect.Sets;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by chambers on 10/28/15.
 */
public class PermissionDaoTests extends AbstractTest {

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    UserDao userDao;

    Permission perm;

    User user;

    @Before
    public void init() {
        PermissionSpec b = new PermissionSpec("project", "avatar");
        b.setDescription("Access to the Avatar project");
        perm = permissionDao.create(b, false);

        UserSpec ub = new UserSpec();
        ub.setUsername("test");
        ub.setPassword("test");
        ub.setFirstName("mr");
        ub.setLastName("test");
        ub.setEmail("test@zorroa.com");

        user = userService.create(ub);
    }

    @Test
    public void testCreate() {
        PermissionSpec b = new PermissionSpec("group", "test");
        b.setDescription("test");

        Permission p = permissionDao.create(b, false);
        assertEquals(p.getName(), b.getName());
        assertEquals(p.getDescription(), b.getDescription());
    }

    @Test
    public void testCreateImmutable() {
        PermissionSpec b = new PermissionSpec("foo", "bar");
        b.setDescription("foo bar");
        Permission p = permissionDao.create(b, true);
        assertTrue(p.isImmutable());
    }

    @Test
    public void testUpdateUserPermission() {
        assertTrue(permissionDao.updateUserPermission("test", "rambo"));
        assertFalse(userDao.hasPermission(user, "user", "test"));
        assertTrue(userDao.hasPermission(user, "user", "rambo"));
    }

    @Test
    public void testGetByNameAndType() {
        Permission p = permissionDao.get("user", "test");
        assertTrue(p.isImmutable());
        assertEquals("user", p.getType());
        assertEquals("test", p.getName());
    }

    @Test
    public void testCount() {
        long count = permissionDao.count();
        PermissionSpec b = new PermissionSpec("foo", "bar").setDescription("bing");
        permissionDao.create(b, true);
        assertEquals(count+1, permissionDao.count());
    }

    @Test
    public void testCountWithFilter() {
        long count = permissionDao.count(new PermissionFilter().setTypes(Sets.newHashSet("user")));
        assertTrue(count > 0);

        PermissionSpec b = new PermissionSpec("foo", "bar").setDescription("bing");
        permissionDao.create(b, true);

        long newCount = permissionDao.count(new PermissionFilter().setTypes(Sets.newHashSet("user")));
        assertEquals(count, newCount);
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
    public void testGetPagedEmptyFilter() {
        PagedList<Permission> perms = permissionDao.getPaged(Paging.first(), new PermissionFilter());
        assertTrue(perms.size() > 0);
    }

    @Test
    public void testGetPagedFiltered() {
        PermissionSpec b = new PermissionSpec("test1", "test2");
        b.setDescription("test");
        permissionDao.create(b, false);

        PagedList<Permission> perms = permissionDao.getPaged(Paging.first(),
                new PermissionFilter().setTypes(Sets.newHashSet("test1")));
        assertEquals(1, perms.size());

        perms = permissionDao.getPaged(Paging.first(),
                new PermissionFilter().setNames(Sets.newHashSet("test2")));
        assertEquals(1, perms.size());

        perms = permissionDao.getPaged(Paging.first(),
                new PermissionFilter().setNames(Sets.newHashSet("test2"))
                        .setTypes(Sets.newHashSet("test1")));
        assertEquals(1, perms.size());
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

    @Test
    public void testUpdate() {
        PermissionSpec b = new PermissionSpec("group", "test").setDescription("foo");
        Permission p = permissionDao.create(b, false);
        assertEquals("group", p.getType());
        assertEquals("test", p.getName());
        assertEquals("foo", p.getDescription());

        p.setType("foo");
        p.setName("bar");
        p.setDescription("bing");

        p = permissionDao.update(p);
        assertEquals("foo", p.getType());
        assertEquals("bar", p.getName());
        assertEquals("bing", p.getDescription());
    }

    @Test
    public void testAttemptUpdateImmutable() {
        Permission p = permissionDao.get("user", "test");
        p.setType("foo");
        p.setName("bar");
        p.setDescription("bing");

        p = permissionDao.update(p);
        assertEquals("user", p.getType());
        assertEquals("test", p.getName());
    }
}
