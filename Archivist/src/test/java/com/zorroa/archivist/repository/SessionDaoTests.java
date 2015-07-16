package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/16/15.
 */
public class SessionDaoTests extends ArchivistApplicationTests {

    @Autowired
    SessionDao sessionDao;

    @Autowired
    UserService userService;

    @Test
    public void testCreate() {
        User user = userService.get(1);
        sessionDao.create(user, "abc123");
        assertEquals("abc123", sessionDao.getAll(user).get(0));
    }

    @Test
    public void testGetAll() {
        User user = userService.get(1);
        assertTrue(sessionDao.getAll(user).isEmpty());

        sessionDao.create(user, "abc123");
        sessionDao.create(user, "efg456");

        assertEquals(2, sessionDao.getAll(user).size());
        assertTrue(sessionDao.getAll(user).contains("abc123"));
        assertTrue(sessionDao.getAll(user).contains("efg456"));
    }

    @Test
    public void testDelete() {
        User user = userService.get(1);
        sessionDao.create(user, "abc123");
        assertTrue(sessionDao.delete("abc123"));
        assertFalse(sessionDao.delete("abc123"));
    }

    @Test
    public void testUpdateLastRequestTime() {
        User user = userService.get(1);
        sessionDao.create(user, "abc123");
        assertTrue(sessionDao.refreshLastRequestTime("abc123"));
    }
}
