package com.zorroa.archivist.repository;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.RoomBuilder;
import com.zorroa.archivist.sdk.domain.RoomUpdateBuilder;
import com.zorroa.archivist.sdk.domain.Session;
import com.zorroa.archivist.sdk.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.junit.Assert.*;

public class RoomDaoTests extends ArchivistApplicationTests {

    @Autowired
    UserService userService;

    @Autowired
    RoomDao roomDao;

    @Autowired
    SessionDao sessionDao;

    Room room;

    @Before
    public void init() {
        RoomBuilder builder = new RoomBuilder();
        builder.setName("a room");
        builder.setVisible(true);
        builder.setPassword("open seasame");
        room = roomDao.create(builder);
    }

    @Test
    public void testGet() {
        Room room2 = roomDao.get(room.getId());
        assertEquals(room.getName(), room2.getName());
    }

    @Test
    public void testGetPassword() {
        // The encrypted password
        String hashed = roomDao.getPassword(room.getId());
        assertTrue(hashed.startsWith("$"));

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("open seasame", hashed));
        assertFalse(BCrypt.checkpw("gtfo", hashed));
    }

    @Test
    public void testGetAllBySession() {

        Session session = sessionDao.create(userService.get(1), "1");

        for (int i=0; i<10; i++) {
            RoomBuilder bld = new RoomBuilder();
            bld.setName("room" + i);
            bld.setVisible(true);
            roomDao.create(bld);
        }

        assertEquals(11, roomDao.getAll(session).size());
    }

    @Test
    public void testJoin() {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("the room");
        bld.setVisible(true);
        Room room = roomDao.create(bld);

        Session session = sessionDao.create(userService.get(1), "1");
        roomDao.join(room, session);
        Room joined = roomDao.get(session);
        assertNotNull(joined);
    }

    @Test
    public void testGetBySession() {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("the room");
        bld.setVisible(true);
        Room room = roomDao.create(bld);

        Session session = sessionDao.create(userService.get(1), "1");
        Room joined = roomDao.get(session);
        assertNull(joined);

        assertTrue(roomDao.join(room, session));

        joined = roomDao.get(session);
        assertEquals(joined.getId(), room.getId());
    }

    @Test
    public void testUpdate() {
        RoomUpdateBuilder updater = new RoomUpdateBuilder();
        updater.setName("test123");
        updater.setPassword("test123");


        // TODO: handle invite list

        assertTrue(roomDao.update(room, updater));

        Room room2 = roomDao.get(room.getId());
        assertEquals(updater.getName(), room2.getName());
        assertTrue(BCrypt.checkpw("test123", roomDao.getPassword(room2.getId())));
    }

    @Test
    public void testDelete() {
        assertTrue(roomDao.delete(room));
        assertFalse(roomDao.delete(room));
    }
}

