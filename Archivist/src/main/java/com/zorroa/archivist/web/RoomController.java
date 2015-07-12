package com.zorroa.archivist.web;

import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@RestController
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    @Autowired
    RoomService roomService;

    /**
     * User joins a particular room
     */
    @RequestMapping(value="/api/v1/rooms/{id}/_join", method=RequestMethod.PUT)
    public void join(@PathVariable long id, HttpSession session) {
        Room room = roomService.get(id);
        roomService.setActiveRoom(session.getId(), room);
    }

    /**
     * Get information for particular room.
     */
    @RequestMapping(value="/api/v1/rooms/{id}", method=RequestMethod.GET)
    public Room get(@PathVariable long id) {
        Room room = roomService.get(id);
        return room;
    }
    /**
     * Return a list of all rooms excluding all non-visible
     * rooms except for the user's personal room.
     * @return
     */
    @RequestMapping(value="/api/v1/rooms", method=RequestMethod.GET)
    public List<Room> getAll() {
        return roomService.getAll();
    }

    /**
     * Create and join a new room.
     *
     * @param builder
     * @return
     */
    @RequestMapping(value="/api/v1/rooms", method=RequestMethod.POST)
    public Room create(@RequestBody RoomBuilder builder, HttpSession session) {
        // Don't allow session rooms
        logger.info("Creating room {}", builder);
        builder.setSession(null);
        Room room = roomService.create(builder);
        roomService.setActiveRoom(session.getId(), room);
        return room;
    }
}
