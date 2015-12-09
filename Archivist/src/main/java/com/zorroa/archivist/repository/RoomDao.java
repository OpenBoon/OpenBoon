package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.*;

import java.util.List;
import java.util.Set;

public interface RoomDao {

    Room create(RoomBuilder builder);

    Room get(long id);
    Room get(Session session);

    boolean update(Room room, RoomUpdateBuilder builder);

    boolean delete(Room room);

    String getPassword(long id);

    List<Room> getAll(Session session);

    boolean join(Room room, Session session);

    /**
     * Get the current selection for a room.
     *
     * @param room
     * @return
     */
    Set<String> getSelection(Room room);

    /**
     * Set the current selection for a room, return the rooms version number.
     *
     * @param room
     * @param selection
     * @return
     */
    int setSelection(Room room, Set<String> selection);

    /**
     * Get the current search for a room.
     *
     * @param room
     * @return
     */
    AssetSearchBuilder getSearch(Room room);

    /**
     * Set the curret search for a room.
     *
     * @param room
     * @param search
     * @return
     */
    int setSearch(Room room, AssetSearchBuilder search);

    /**
     * A convenience method for pulling down the state of a room
     * with a single API call.
     * @param room
     * @return
     */
    SharedRoomState getSharedState(Room room);
}
