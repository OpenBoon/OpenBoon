package com.zorroa.archivist.service;

import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.RoomBuilder;
import com.zorroa.archivist.sdk.domain.Session;
import com.zorroa.archivist.sdk.domain.User;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 7/14/15.
 */
public class RoomServiceTests extends ArchivistApplicationTests {

    @Autowired
    RoomService roomService;

    @Autowired
    SessionDao sessionDao;

    @Autowired
    UserService userService;

    @Test
    public void testCreate() {
        RoomBuilder builder = new RoomBuilder();
        builder.setName("Sekret Spot");
        Room room = roomService.create(builder);
        assertEquals(builder.getName(), room.getName());
    }

    @Test
    public void testJoin() {
        RoomBuilder builder = new RoomBuilder();
        builder.setName("Water Cooler");
        Room room = roomService.create(builder);
        User user = userService.get(1);
        Session session = sessionDao.create(user, "def456");
        boolean ok = roomService.join(room, session);
        assertTrue(ok);
        room = roomService.get(session);
        assertEquals("Water Cooler", room.getName());
    }
}
