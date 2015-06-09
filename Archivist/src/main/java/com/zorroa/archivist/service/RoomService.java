package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Message;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

public interface RoomService {

    Room create(RoomBuilder bld);

    void setActiveRoom(Room room);

    Room getActiveRoom();

    void sendToActiveRoom(Message message);

}
