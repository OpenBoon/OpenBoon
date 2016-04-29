package com.zorroa.archivist.service;

import com.zorroa.sdk.domain.*;

import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 12/15/15.
 */
public interface RoomService {

    /**
     * The maximum size of a selection that can be propagated to a room.  This will
     * probably need to be tweaked/tested for very large selections, but 1024 gives
     * us roughly a 32k packet size.
     */
    int SELECTION_MAX_SIZE = 1024;

    Room get(long id);

    Room create(RoomBuilder bld);

    /**
     * Get the sessions active room or return null if there
     * is no active room.
     *
     * @param session
     * @return
     */
    Room getActiveRoom(Session session);

    /**
     * Return the active room for the currently logged in session.  Return
     * null of the session has no room.
     *
     * @return
     */
    Room getActiveRoom();

    /**
     * Joins the active session to the given room.
     * @param room
     * @return
     */
    boolean join(Room room);

    /**
     * Joins the given session to the given room.
     * @param room
     * @return
     */
    boolean join(Room room, Session session);

    /**
     * Removes the given session from a room.
     *
     * @param room
     * @param session
     * @return
     */
    boolean leave(Room room, Session session);

    /**
     * Removes the active session from the given room.
     *
     * @return
     */
    boolean leave(Room room);

    /**
     * Get all rooms.
     *
     * @return
     */
    List<Room> getAll();

    /**
     * Update existing room.
     *
     * @param room
     * @param updater
     * @return
     */
    boolean update(Room room, RoomUpdateBuilder updater);

    /**
     * Delete existing room.
     *
     * @param room
     * @return
     */
    boolean delete(Room room);

    /**
     * Set the current selection for the given room and
     * emit the selection into the event stream.
     *
     * @param room
     * @param selection
     * @return
     */
    int setSelection(Room room, Set<String> selection);

    /**
     * Set the current search for the room and emit the current
     * search into the event stream.   Setting a new search
     * also clears the current selection.
     *
     * @param room
     * @param search
     * @return
     */
    int setSearch(Room room, AssetSearch search);

    /**
     * Return the current Asset selection in the room.
     *
     * @param room
     * @return
     */
    Set<String> getSelection(Room room);

    /**
     * Return the current search being viewed in the room.
     *
     * @param room
     * @return
     */
    AssetSearch getSearch(Room room);

    /**
     * Return all data that is part of the shared state of the room.
     *
     * @param room
     * @return
     */
    SharedRoomState getSharedState(Room room);
}
