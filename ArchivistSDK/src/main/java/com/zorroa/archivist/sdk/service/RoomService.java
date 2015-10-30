package com.zorroa.archivist.sdk.service;

import com.zorroa.archivist.sdk.domain.*;

import java.util.List;

public interface RoomService {

    Room get(long id);

    Room create(RoomBuilder bld);

    Room getActiveRoom(Session session);

    Room get(Session session);

    void sendToRoom(Room room, Message message);

    boolean join(Room room, Session session);

    List<Room> getAll(Session session);

    boolean update(Room room, RoomUpdateBuilder updater);

    boolean delete(Room room);
}
