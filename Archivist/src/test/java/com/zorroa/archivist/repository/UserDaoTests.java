package com.zorroa.archivist.repository;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.StandardRoles;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;

public class UserDaoTests extends ArchivistApplicationTests {

    @Autowired
    UserDao userDao;

    User user;

    @Before
    public void init() {
        UserBuilder builder = new UserBuilder();
        builder.setUsername("test");
        builder.setPassword("test");
        builder.setEmail("test@test.com");
        builder.setRoles(Sets.newHashSet(StandardRoles.USER));
        user = userDao.create(builder);
    }

    @Test
    public void testGet() {
        User user2 = userDao.get(user.getId());
        assertEquals(user.getId(), user2.getId());
    }

    @Test
    public void testAll() {
        assertEquals(3, userDao.getAll().size());

        UserBuilder builder = new UserBuilder();
        builder.setUsername("foo");
        builder.setPassword("test");
        builder.setEmail("test@test.com");
        builder.setRoles(Sets.newHashSet(StandardRoles.USER));
        userDao.create(builder);

        assertEquals(4, userDao.getAll().size());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetFailed() {
        userDao.get("blah");
    }

    @Test
    public void testGetPassword() {
        // The crypted password
        String hashed = userDao.getPassword(user.getUsername());
        assertTrue(hashed.startsWith("$"));

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("test", hashed));
        assertFalse(BCrypt.checkpw("gtfo", hashed));
    }
}
