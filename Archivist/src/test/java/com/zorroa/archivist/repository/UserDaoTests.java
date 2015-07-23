package com.zorroa.archivist.repository;

import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.List;

import static org.junit.Assert.*;

public class UserDaoTests extends ArchivistApplicationTests {

    @Autowired
    UserDao userDao;

    @Autowired
    RoomDao roomDao;

    @Autowired
    SessionDao sessionDao;

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

    @Test
    public void testUpdate() {
        UserUpdateBuilder builder = new UserUpdateBuilder();
        builder.setUsername("foo");
        builder.setPassword("bar");
        builder.setEmail("test@test.com");

        assertTrue(userDao.update(user, builder));
        User updated = userDao.get(user.getId());
        assertEquals(builder.getEmail(), updated.getEmail());
        assertEquals(builder.getUsername(), updated.getUsername());

        assertTrue(BCrypt.checkpw("bar", userDao.getPassword("foo")));
    }

    @Test
    public void testGetUsers() {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("the room");
        bld.setVisible(true);
        Room room = roomDao.create(bld);

        Session session = sessionDao.create(userDao.get(1), "1");
        roomDao.join(room, session);

        List<User> users = userDao.getAll(room);
        assertEquals(1, users.size());
        assertTrue(users.contains(userDao.get(1)));
    }
}
