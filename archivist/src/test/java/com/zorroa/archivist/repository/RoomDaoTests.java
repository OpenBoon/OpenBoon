package com.zorroa.archivist.repository;

import com.google.common.collect.Sets;
import com.zorroa.archivist.AbstractTest;
import com.zorroa.sdk.domain.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Set;

import static org.junit.Assert.*;

public class RoomDaoTests extends AbstractTest {

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
    public void testCreateWithSearchAndSelection() {
        RoomBuilder builder = new RoomBuilder();
        builder.setName("a room");
        builder.setVisible(true);
        builder.setSearch(new AssetSearch("foo"));
        builder.setSelection(Sets.newHashSet("a", "b", "c"));
        Room room1 = roomDao.create(builder);

        AssetSearch asb2 = roomDao.getSearch(room1);
        assertEquals(builder.getSearch().getQuery(), asb2.getQuery());

        assertEquals(builder.getSelection(), roomDao.getSelection(room1));
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

    @Test
    public void testGetAndSetSearch() {
        roomDao.setSelection(room, Sets.newHashSet("a", "b", "c"));

        AssetSearch asb1 = new AssetSearch("foo");
        roomDao.setSearch(room, asb1);

        AssetSearch asb2 = roomDao.getSearch(room);
        assertEquals(asb1.getQuery(), asb2.getQuery());

        Set<String> selection = roomDao.getSelection(room);
        assertTrue(selection.isEmpty());
    }

    @Test
    public void testGetAndSetSelection() {
        Set<String> selection = Sets.newHashSet("a", "b", "c");
        roomDao.setSelection(room, selection);
        assertEquals(selection, roomDao.getSelection(room));
    }

    @Test
    public void testGetSharedRoomState() {
        Set<String> selection1 = Sets.newHashSet("a", "b", "c");
        AssetSearch asb1 = new AssetSearch("foo");

        roomDao.setSearch(room, asb1);
        roomDao.setSelection(room, selection1);

        SharedRoomState state = roomDao.getSharedState(room);
        assertEquals(selection1, state.getSelection());
        assertEquals(asb1.getQuery(), state.getSearch().getQuery());
    }

    @Test
    public void testGetEmptyRoomState() {
        SharedRoomState state = roomDao.getSharedState(room);
        Set<String> emptySelection = Sets.newHashSet();
        assertEquals(emptySelection, state.getSelection());
        assertEquals(null, state.getSearch());
    }
}

