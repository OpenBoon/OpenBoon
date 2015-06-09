package com.zorroa.archivist.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.service.RoomService;

public class RoomController {

    @Autowired
    RoomService roomService;

    /**
     * User joins a particular room
     */
    @RequestMapping(value="/rooms/{id}/_join", method=RequestMethod.POST)
    public void joinRoom(@PathVariable long id) {
        Room room = roomService.get(id);
        roomService.setActiveRoom(room);
    }

    @RequestMapping(value="/rooms", method=RequestMethod.POST)
    public Room ingest(@RequestBody RoomBuilder builder) {
        // Don't allow session rooms
        builder.setSession(null);
        Room room = roomService.create(builder);
        roomService.setActiveRoom(room);
        return room;
    }
}
