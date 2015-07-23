package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.service.RoomService;
import com.zorroa.archivist.service.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoomControllerTests extends MockMvcTest {

    @Autowired
    RoomController roomController;

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Test
    public void testCreate() throws Exception {
        RoomBuilder bld = new RoomBuilder();
        bld.setName("A Room");
        bld.setVisible(true);
        bld.setInviteList(Sets.newHashSet("mvchambers@me.com"));

        MvcResult result = mvc.perform(post("/api/v1/rooms")
                 .session(admin())
                 .contentType(MediaType.APPLICATION_JSON_VALUE)
                 .content(Json.serialize(bld)))
                 .andExpect(status().isOk())
                 .andReturn();

        Room room = Json.deserialize(result.getResponse().getContentAsByteArray(), Room.class);
        assertEquals(bld.getName(), room.getName());
        assertEquals(bld.getInviteList(), room.getInviteList());
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
        assertEquals(10, rooms.size());
    }

    @Test
    public void testJoin() throws Exception {

        MockHttpSession httpSession = admin();
        Session session = userService.getSession(httpSession);

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
        Session session1 = userService.getSession(admin);

        MockHttpSession user = user();
        Session session2 = userService.getSession(user);

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
