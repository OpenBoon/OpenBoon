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
        builder.setUserId("test");
        builder.setPassword("test");
        builder.setRoles(Sets.newHashSet(StandardRoles.USER));
        user = userDao.create(builder);
    }

    @Test
    public void testGet() {
        User user2 = userDao.get(user.getId());
        assertEquals(user.getId(), user2.getId());
    }

    @Test(expected=EmptyResultDataAccessException.class)
    public void testGetFailed() {
        userDao.get("blah");
    }

    @Test
    public void testGetPassword() {
        // The crypted password
        String hashed = userDao.getPassword(user.getId());
        assertTrue(hashed.startsWith("$"));

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("test", hashed));
        assertFalse(BCrypt.checkpw("gtfo", hashed));
    }
}
