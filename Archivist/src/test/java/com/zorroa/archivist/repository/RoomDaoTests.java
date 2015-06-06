package com.zorroa.archivist.repository;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

public class RoomDaoTests extends ArchivistApplicationTests {

    @Autowired
    RoomDao roomDao;

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
        // The crypted password
        String hashed = roomDao.getPassword(room.getId());
        assertTrue(hashed.startsWith("$"));

        // try to authenticate it.
        assertTrue(BCrypt.checkpw("open seasame", hashed));
        assertFalse(BCrypt.checkpw("gtfo", hashed));
    }
}
