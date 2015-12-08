package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.*;

import java.util.List;
import java.util.Set;

public interface RoomService {

    Room get(long id);

    Room create(RoomBuilder bld);

    Room getActiveRoom(Session session);

    /**
     * Return the active room for the currently logged in session.  Return
     * null of the session has no room.
     *
     * @return
     */
    Room getActiveRoom();

    Room get(Session session);

    void sendToRoom(Room room, Message message);

    boolean join(Room room, Session session);

    List<Room> getAll(Session session);

    boolean update(Room room, RoomUpdateBuilder updater);

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
     * search into the event stream.
     *
     * @param room
     * @param search
     * @return
     */
    int setSearch(Room room, AssetSearchBuilder search);

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
    AssetSearchBuilder getSearch(Room room);

    /**
     * Return all data that is part of the shared state of the room.
     *
     * @param room
     * @return
     */
    SharedRoomState getSharedState(Room room);
}
