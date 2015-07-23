package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.domain.Session;

import java.util.List;

public interface RoomService {

    Room get(long id);

    Room create(RoomBuilder bld);

    Room getActiveRoom(Session session);

    Room get(Session session);

    void sendToRoom(Room room, Message message);

    boolean join(Room room, Session session);

    List<Room> getAll(Session session);
}
