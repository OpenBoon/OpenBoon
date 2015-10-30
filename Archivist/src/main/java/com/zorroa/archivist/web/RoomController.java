package com.zorroa.archivist.web;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
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

    @Autowired
    UserService userService;

    /**
     * User joins a particular room
     */
    @RequestMapping(value="/api/v1/rooms/{id}/_join", method=RequestMethod.PUT)
    public void join(@PathVariable long id, HttpSession httpSession) {
        Room room = roomService.get(id);
        Session session = userService.getActiveSession();
        logger.info("Session {} is joining room:{}", session.getId(), room.getId());
        roomService.join(room, session);
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
    public List<Room> getAll(HttpSession httpSession) {
        Session session = userService.getActiveSession();
        return roomService.getAll(session);
    }

    /**
     * Create and join a new room.
     *
     * @param builder
     * @return
     */
    @RequestMapping(value="/api/v1/rooms", method=RequestMethod.POST)
    public Room create(@RequestBody RoomBuilder builder, HttpSession httpSession) {
        // Don't allow session rooms
        logger.info("Creating room {}", builder);
        Room room = roomService.create(builder);

        Session session = userService.getActiveSession();
        roomService.join(room, session);
        return room;
    }

    /**
     * Get all the users for a given room.
     *
     * @param id
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/{id}/users", method=RequestMethod.GET)
    public List<User> users(@PathVariable long id) {
        Room room = roomService.get(id);
        return userService.getAll(room);
    }

    @RequestMapping(value="/api/v1/rooms/{id}", method=RequestMethod.PUT)
    public Room update(@RequestBody RoomUpdateBuilder builder, @PathVariable int id, HttpSession httpSession) {
        Session session = userService.getActiveSession();

        if (session.getUserId() == id || SecurityUtils.hasPermission("manager", "systems")) {
            Room room = roomService.get(id);
            roomService.update(room, builder);
            return roomService.get(id);
        }
        else {
            throw new SecurityException("You do not have the access to modify this room.");
        }
    }

    @RequestMapping(value="/api/v1/rooms/{id}", method=RequestMethod.DELETE)
    public boolean delete(@PathVariable int id) {
        Room room = roomService.get(id);
        // TODO: what if people are in the room.
        return roomService.delete(room);
    }
}
