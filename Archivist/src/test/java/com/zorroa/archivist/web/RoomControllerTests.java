package com.zorroa.archivist.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.archivist.Json;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.service.RoomService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoomControllerTests extends MockMvcTest {

    @Autowired
    RoomController roomController;

    @Autowired
    RoomService roomService;

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
                result.getResponse().getContentAsByteArray(), new TypeReference<List<Room>>(){});
        assertEquals(10, rooms.size());
    }

    @Test
    public void testJoin() throws Exception {

        MockHttpSession session = admin();

        for (int i=0; i<10; i++) {
            RoomBuilder bld = new RoomBuilder();
            bld.setName("Room #" + i);
            bld.setVisible(true);
            Room room = roomService.create(bld);

            mvc.perform(put("/api/v1/rooms/" + room.getId() + "/_join")
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();

            Room room2 = roomService.getActiveRoom(session.getId());
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
}
