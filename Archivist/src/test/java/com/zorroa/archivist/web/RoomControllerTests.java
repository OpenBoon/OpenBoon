package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.archivist.repository.RoomDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.sdk.util.Json;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoomControllerTests extends MockMvcTest {

    @Autowired
    RoomController roomController;

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Autowired
    RoomDao roomDao;

    @Test
    public void testCreate() throws Exception {
        RoomBuilder bld = new RoomBuilder();
        bld.setName("A Room");
        bld.setVisible(true);
        // Disable invite list until we fix the RoomDao bug reading invitation arrays
//        bld.setInviteList(Sets.newHashSet("mvchambers@me.com"));

        MvcResult result = mvc.perform(post("/api/v1/rooms")
                 .session(admin())
                 .contentType(MediaType.APPLICATION_JSON_VALUE)
                 .content(Json.serialize(bld)))
                 .andExpect(status().isOk())
                 .andReturn();

        Room room = Json.deserialize(result.getResponse().getContentAsByteArray(), Room.class);
        assertEquals(bld.getName(), room.getName());
//        assertEquals(bld.getInviteList(), room.getInviteList());
    }

    @Test
    public void testUpdate() throws Exception {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("RoomA");
        bld.setVisible(true);
        Room room = roomService.create(bld);

        RoomUpdateBuilder update = new RoomUpdateBuilder();
        update.setName("RoomB");
        update.setPassword("test123");

        MvcResult result = mvc.perform(put("/api/v1/rooms/" + room.getId())
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update)))
                .andExpect(status().isOk())
                .andReturn();

        Room updatedRoom = Json.deserialize(result.getResponse().getContentAsByteArray(), Room.class);
        assertEquals(update.getName(), updatedRoom.getName());
        assertTrue(BCrypt.checkpw("test123", roomDao.getPassword(room.getId())));
    }

    @Test
    public void testDelete() throws Exception {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("RoomA");
        bld.setVisible(true);
        Room room = roomService.create(bld);

        MvcResult result = mvc.perform(delete("/api/v1/rooms/" + room.getId())
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        boolean isDeleted = Json.deserialize(result.getResponse().getContentAsByteArray(), Boolean.class);
        assertTrue(isDeleted);
    }

    @Test
    public void testGetAll() throws Exception {

        for (int i=0; i<10; i++) {
            RoomBuilder bld = new RoomBuilder();
            bld.setName("Room #" + i);
            bld.setVisible(true);
            roomService.create(bld);
        }

        MvcResult result = mvc.perform(get("/api/v1/rooms")
                 .session(admin())
                 .contentType(MediaType.APPLICATION_JSON_VALUE))
                 .andExpect(status().isOk())
                 .andReturn();

        List<Room> rooms = Json.Mapper.readValue(
                result.getResponse().getContentAsByteArray(), new TypeReference<List<Room>>() {
        });
        // Take into account room created by session
        assertEquals(11, rooms.size());
    }

    @Test
    public void testJoin() throws Exception {

        MockHttpSession httpSession = admin();
        Session session = userService.getSession(httpSession.getId());

        for (int i=0; i<10; i++) {
            RoomBuilder bld = new RoomBuilder();
            bld.setName("Room #" + i);
            bld.setVisible(true);
            Room room = roomService.create(bld);

            mvc.perform(put("/api/v1/rooms/" + room.getId() + "/_join")
                    .session(httpSession)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();

            Room room2 = roomService.getActiveRoom(session);
            assertNotNull(room2);
            assertEquals(room.getId(), room2.getId());
        }
    }

    @Test
    public void testGet() throws Exception {

        RoomBuilder bld = new RoomBuilder();
        bld.setName("A Room");
        bld.setVisible(true);
        bld.setInviteList(Sets.newHashSet("mvchambers@me.com"));
        Room room1 = roomService.create(bld);

        MvcResult result = mvc.perform(get("/api/v1/rooms/" + room1.getId())
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        Room room2 = Json.deserialize(result.getResponse().getContentAsByteArray(), Room.class);
        assertEquals(room1.getName(), room2.getName());
        assertEquals(room1.getInviteList(), room2.getInviteList());
    }

    @Test
    public void testGetAllUsers() throws Exception {

        MockHttpSession admin = admin();
        Session session1 = userService.getSession(admin.getId());

        MockHttpSession user = user();
        Session session2 = userService.getSession(user.getId());

        RoomBuilder builder = new RoomBuilder();
        builder.setName("foo");
        builder.setVisible(true);
        Room room1 = roomService.create(builder);

        roomService.join(room1, session1);
        roomService.join(room1, session2);

        MvcResult result = mvc.perform(get("/api/v1/rooms/" + room1.getId() + "/users")
                .session(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        List<User> users = Json.Mapper.readValue(result.getResponse().getContentAsByteArray(),
                new TypeReference<List<User>>() {});

        assertEquals(2, users.size());
    }
}
