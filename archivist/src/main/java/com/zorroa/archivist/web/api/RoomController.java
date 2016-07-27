package com.zorroa.archivist.web.api;

import com.google.common.collect.ImmutableMap;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.service.RoomService;
import com.zorroa.archivist.service.SearchService;
import com.zorroa.archivist.service.UserService;
import com.zorroa.sdk.domain.*;
import com.zorroa.sdk.search.AssetSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
public class RoomController {

    private static final Logger logger = LoggerFactory.getLogger(RoomController.class);

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Autowired
    SearchService searchService;

    /**
     * User joins a particular room
     */
    @RequestMapping(value="/api/v1/rooms/{id}/_join", method=RequestMethod.PUT)
    public Object join(@PathVariable long id, HttpSession httpSession) {
        Room room = roomService.get(id);
        return ImmutableMap.of("roomId", room.getId(), "result", roomService.join(room));
    }

    /**
     * User leaves the current room
     */
    @RequestMapping(value="/api/v1/rooms/current/_leave", method=RequestMethod.PUT)
    public Object leave(HttpSession httpSession) {
        Room room = roomService.getActiveRoom();
        if (room != null) {
            return ImmutableMap.of("roomId", room.getId(), "result", roomService.leave(room));
        }
        else {
            return ImmutableMap.of("roomId", -1, "result", true);
        }
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
    public Room create(@RequestBody RoomBuilder builder, HttpSession httpSession) {
        Room room = roomService.create(builder);
        roomService.join(room);
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

        if (session.getUserId() == id || SecurityUtils.hasPermission("group::manager", "group::systems")) {
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

    /**
     * Set the current room selection, returns the new version number of the room.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/current/selection", method=RequestMethod.PUT)
    public void setSelection(@RequestBody Set<String> assetIds) {
        roomService.setSelection(roomService.getActiveRoom(), assetIds);
    }

    /**
     * Return the selected assetIds for the users current room.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/current/selection", method=RequestMethod.GET)
    public Set<String> getSelection() {
        return roomService.getSelection(roomService.getActiveRoom());
    }

    /**
     * Return just the current search.  The current search is set simply by searching, so there
     * is no method to set a room search currently.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/current/search", method=RequestMethod.GET)
    public AssetSearch getSearch() {
        return roomService.getSearch(roomService.getActiveRoom());
    }

    /**
     * Set the current search property on a room.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/current/search", method=RequestMethod.PUT)
    public void setSearch(@RequestBody AssetSearch search) {
        roomService.setSearch(roomService.getActiveRoom(), search);
    }

    /**
     * Return the full shared room state.  Current this includes the current
     * search, selection, and state version however it might contain other
     * data in the future.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/current/state", method=RequestMethod.GET)
    public SharedRoomState getSharedState() {
        return roomService.getSharedState(roomService.getActiveRoom());
    }


    /**
     * Return the full shared room state.  Current this includes the current
     * search, selection, and state version however it might contain other
     * data in the future.
     *
     * @return
     */
    @RequestMapping(value="/api/v1/rooms/current/assets", method=RequestMethod.GET)
    public void getAssets(HttpSession httpSession, HttpServletResponse httpResponse) throws IOException {
        AssetSearch search = roomService.getSearch(roomService.getActiveRoom());
        HttpUtils.writeElasticResponse(searchService.search(search), httpResponse);
    }
}
