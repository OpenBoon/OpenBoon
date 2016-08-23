package com.zorroa.archivist.repository;

import com.google.common.collect.Sets;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.RoomBuilder;
import com.zorroa.sdk.domain.Session;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/16/15.
 */
public class SessionDaoTests extends AbstractTest {

    @Autowired
    SessionDao sessionDao;

    @Test
    public void testCreate() {
        User user = userService.get(1);
        sessionDao.create(user, "abc123");
        assertEquals("abc123", sessionDao.getAll(user).get(0).getCookieId());
    }

    @Test
    public void testGetAll() {
        User user = userService.get(1);
        assertTrue(sessionDao.getAll(user).isEmpty());

        sessionDao.create(user, "abc123");
        sessionDao.create(user, "efg456");

        assertEquals(2, sessionDao.getAll(user).size());

        Set<String> ids = Sets.newHashSet();
        ids.addAll(sessionDao.getAll(user).stream().map(
                Session::getCookieId).collect(Collectors.toList()));

        assertTrue(ids.contains("abc123"));
        assertTrue(ids.contains("efg456"));
    }

    @Test
    public void testGetAllByRoom() {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("the room");
        bld.setVisible(true);
        Room room = roomService.create(bld);

        Session session = sessionDao.create(userService.get(1), "1");
        roomService.join(room, session);

        List<Session> sessions = sessionDao.getAll(room);
        assertEquals(1, sessions.size());
        assertTrue(sessions.contains(session));
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

    @Test
    public void testGetByCookie() {
        User user = userService.get(1);
        Session session1 = sessionDao.create(user, "abc123");

        Session session2 = sessionDao.get(session1.getCookieId());
        assertEquals(session1, session2);
    }

    @Test
    public void testGetById() {
        User user = userService.get(1);
        Session session1 = sessionDao.create(user, "abc123");

        Session session2 = sessionDao.get(session1.getId());
        assertEquals(session1, session2);
    }
}
