package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Permission;
import com.zorroa.archivist.domain.PermissionBuilder;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.Assert.assertEquals;
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
        b.setName("avatar");
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
        b.setName("test");
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
    public void testSetPermissions() {
        permissionDao.setPermissions(user, Lists.newArrayList(permissionDao.get("manager")));
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(permissionDao.get("manager")));
    }

    @Test
    public void testSetPermissionsVargs() {
        permissionDao.setPermissions(user, permissionDao.get("manager"));
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(permissionDao.get("manager")));
    }

    @Test
    public void getAllAuthorities() {
        List<GrantedAuthority> auths = permissionDao.getGrantedAuthorities(user);
        assertTrue(auths.size() == 0);

        permissionDao.setPermissions(user, permissionDao.getAll());
        auths = permissionDao.getGrantedAuthorities(user);
        assertTrue(auths.size() == permissionDao.getAll().size());
    }
}
