package com.zorroa.archivist.service;

import com.google.common.collect.Sets;
import com.zorroa.archivist.ArchivistApplicationTests;
import com.zorroa.archivist.repository.SessionDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.exception.MalformedDataException;
import com.zorroa.archivist.security.SecurityUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Created by chambers on 7/14/15.
 */
public class RoomServiceTests extends ArchivistApplicationTests {

    @Autowired
    RoomService roomService;

    @Autowired
    SessionDao sessionDao;

    @Autowired
    SessionRegistry sessionRegistry;

    Room room;
    Session session;

    @Before
    public void init() throws Exception {
        room = roomService.create(new RoomBuilder("test"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        sessionRegistry.registerNewSession(request.getSession().getId(), SecurityUtils.getUser());
        session = userService.getSession(SecurityUtils.getCookieId());
    }

    @Test
    public void testCreate() {
        RoomBuilder builder = new RoomBuilder();
        builder.setName("test");
        Room room = roomService.create(builder);
        assertEquals(builder.getName(), room.getName());
    }

    @Test
    public void testGet() {
        RoomBuilder builder = new RoomBuilder();
        builder.setName("test");
        Room room1 = roomService.create(builder);
        Room room2 = roomService.get(room1.getId());
        assertEquals(room1.getId(), room2.getId());
    }

    @Test
    public void testGetActiveRoom() {
        assertNull(roomService.getActiveRoom());
        assertNull(roomService.getActiveRoom(session));
        assertTrue(roomService.join(room, session));
        assertEquals(room.getId(), roomService.getActiveRoom().getId());
    }

    @Test
    public void join() {
        assertTrue(roomService.join(room, session));
        assertFalse(roomService.join(room, session));
    }

    @Test
    public void joinActive() {
        assertTrue(roomService.join(room));
        assertFalse(roomService.join(room));
    }

    @Test
    public void leaveActive() {
        assertTrue(roomService.join(room));
        assertTrue(roomService.leave(room));
        assertFalse(roomService.leave(room));
    }

    @Test
    public void leave() {
        assertTrue(roomService.join(room, session));
        assertTrue(roomService.leave(room, session));
        assertFalse(roomService.leave(room, session));
    }

    @Test
    public void update() {
        RoomUpdateBuilder update = new RoomUpdateBuilder();
        update.setName("bob");
        update.setPassword("password");

        assertTrue(roomService.update(room, update));

        Room room2 = roomService.get(room.getId());
        assertEquals(room2.getName(), update.getName());
        // TODO: check password
    }

    @Test
    public void delete() {
        assertTrue(roomService.delete(room));
        assertFalse(roomService.delete(room));
    }

    @Test(expected= MalformedDataException.class)
    public void setSelectionTooLarge() {
        Set<String> selection = Sets.newHashSetWithExpectedSize(RoomService.SELECTION_MAX_SIZE+1);
        for (int i=0; i<RoomService.SELECTION_MAX_SIZE+1; i++) {
            selection.add("selection" + i);
        }
        roomService.setSelection(room, selection);
    }

    @Test
    public void setSelection() {
        Set<String> selection = Sets.newHashSetWithExpectedSize(10);
        for (int i=0; i<10; i++) {
            selection.add("selection" + i);
        }
        int version1 = roomService.setSelection(room, selection);
        int version2 = roomService.setSelection(room, selection);
        assertEquals(version1+1, version2);
        assertEquals(-1, roomService.setSelection(null, selection));
    }

    @Test
    public void setSearch() {
        AssetSearch search = new AssetSearch("foo");
        int version1 = roomService.setSearch(room, search);
        int version2 = roomService.setSearch(room, search);
        assertEquals(version1+1, version2);
        assertEquals(-1, roomService.setSearch(null, search));
    }

}
