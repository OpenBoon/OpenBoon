package com.zorroa.archivist.service;

import java.util.List;

import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

public interface RoomService {

    Room get(long id);

    Room create(RoomBuilder bld);

    void setActiveRoom(String sessionId, Room room);

    void sendToRoom(Room room, Message message);

    List<Room> getAll();

    Room getActiveRoom(String sessionId);
}
