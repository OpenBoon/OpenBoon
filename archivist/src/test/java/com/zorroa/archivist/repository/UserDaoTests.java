package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.sdk.security.Groups;
import com.zorroa.sdk.domain.Pager;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

import static org.junit.Assert.*;

public class UserDaoTests extends AbstractTest {

    @Autowired
    UserDao userDao;

    @Autowired
    PermissionDao permissionDao;

    User user;

    @Before
    public void init() {
        UserSpec builder = new UserSpec();
        builder.setUsername("test");
        builder.setPassword("test");
        builder.setEmail("test@test.com");
        builder.setHomeFolderId(Folder.ROOT_ID);
        builder.setUserPermissionId(permissionDao.get("zorroa", "manager").getId());
        user = userDao.create(builder);
    }

    @Test
    public void testGet() {
        User user2 = userDao.get(user.getId());
        assertEquals(user.getId(), user2.getId());
    }

    @Test
    public void testGetByUsername() {
        User user2 = userDao.get(user.getUsername());
        User user3 = userDao.get(user.getEmail());
        assertEquals(user2.getId(), user3.getId());
        assertEquals(user2.toString(), user3.toString());
    }

    @Test
    public void testGetCount() {
        long count = userDao.getCount();
        assertEquals(count, userDao.getCount());
        UserSpec builder = new UserSpec();
        builder.setUsername("test2");
        builder.setPassword("test2");
        builder.setEmail("shizzle@test.com");
        builder.setHomeFolderId(Folder.ROOT_ID);
        builder.setUserPermissionId(permissionDao.get("zorroa", "manager").getId());
        user = userDao.create(builder);
        assertEquals(++count, userDao.getCount());
    }

    @Test
    public void testAll() {
        assertEquals(4, userDao.getAll().size());

        UserSpec builder = new UserSpec();
        builder.setUsername("foo");
        builder.setPassword("test");
        builder.setEmail("mcbizzile@test.com");
        builder.setHomeFolderId(Folder.ROOT_ID);
        builder.setUserPermissionId(permissionDao.get("zorroa", "manager").getId());
        userDao.create(builder);

        assertEquals(5, userDao.getAll().size());
    }

    @Test
    public void testAllPageable() {
        assertEquals(4, userDao.getAll(Pager.first()).size());
        assertEquals(0, userDao.getAll(new Pager(2, 4)).size());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetFailed() {
        userDao.get("blah");
    }

    @Test
    public void testGetPassword() {
        // The crypted password
        String hashed = userDao.getPassword(user.getUsername());
        String hashed2 = userDao.getPassword(user.getEmail());
        assertEquals(hashed, hashed2);

        assertTrue(hashed.startsWith("$"));

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("test", hashed));
        assertFalse(BCrypt.checkpw("gtfo", hashed));
    }

    @Test
    public void testResetPassword() {
        assertTrue(userDao.setPassword(user, "fiddlesticks"));
        assertTrue(BCrypt.checkpw("fiddlesticks", userDao.getPassword(user.getUsername())));
        assertFalse(BCrypt.checkpw("smeagol", userDao.getPassword(user.getUsername())));
    }

    @Test
    public void testUpdate() {
        UserProfileUpdate builder = new UserProfileUpdate();
        builder.setFirstName("foo");
        builder.setLastName("bar");
        builder.setEmail("test@test.com");

        assertTrue(userDao.update(user, builder));
        User updated = userDao.get(user.getId());
        assertEquals(builder.getEmail(), updated.getEmail());
        assertEquals(builder.getFirstName(), updated.getFirstName());
        assertEquals(builder.getLastName(), updated.getLastName());
    }

    @Test
    public void testSetEnabled() {
        assertTrue(userDao.setEnabled(user, false));
        assertFalse(userDao.setEnabled(user, false));
    }

    @Test
    public void testExists() {
        assertTrue(userDao.exists(user.getUsername(), null));
        assertTrue(userDao.exists(user.getUsername(), "local"));
        assertFalse(userDao.exists(user.getUsername(), "ldap"));
        assertFalse(userDao.exists("sibawitzawis", null));
    }

    @Test
    public void testHasPermissionUningNames() {
        assertFalse(userDao.hasPermission(user, "zorroa", "manager"));
        userDao.addPermission(user, permissionDao.get(Groups.MANAGER), false);
        assertTrue(userDao.hasPermission(user, "zorroa", "manager"));
        assertFalse(userDao.hasPermission(user, "a", "b"));
    }

    @Test
    public void testHasPermission() {
        assertFalse(userDao.hasPermission(user, "zorroa", "manager"));
        userDao.addPermission(user, permissionDao.get(Groups.MANAGER), false);
        assertTrue(userDao.hasPermission(user, permissionDao.get("zorroa", "manager")));
        assertFalse(userDao.hasPermission(user, permissionDao.get("zorroa", "administrator")));
    }

    @Test
    public void testAddPermission() {
        userDao.addPermission(user, permissionDao.get(Groups.MANAGER), false);
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(permissionDao.get(Groups.MANAGER)));
    }

    @Test
    public void testSetPermissions() {
        Permission p = permissionDao.get(Groups.MANAGER);
        assertEquals(1, userDao.setPermissions(user, Lists.newArrayList(p), "local"));
        List<Permission> perms = permissionDao.getAll(user);
        assertTrue(perms.contains(p));
    }

    @Test
    public void testGetUserByPasswordToken() {
        String token = userDao.setEnablePasswordRecovery(user);
        User user2 = userDao.getByToken(token);
        assertEquals(user.getId(), user2.getId());
    }

    @Test
    public void testSetForgotPassword() {
        String token = userDao.setEnablePasswordRecovery(user);
        assertEquals(64, token.length());
        assertEquals(token,
                jdbc.queryForObject("SELECT str_reset_pass_token FROM users WHERE pk_user=?",
                        String.class, user.getId()));
    }

    @Test
    public void testSetResetPassword() {
        assertFalse(userDao.resetPassword(user, "ABC123", "FOO"));
        String token = userDao.setEnablePasswordRecovery(user);

        assertFalse(userDao.resetPassword(user,"BAD_TOKEN", "FOO"));
        assertTrue(userDao.resetPassword(user, token, "FOO"));
    }

    @Test
    public void testIncrementLoginCount() {
        userDao.incrementLoginCounter(user);
        User user2 = userDao.get(user.getId());
        assertTrue(user2.getTimeLastLogin() > 0);
        assertEquals(1, user2.getLoginCount());

    }
}
