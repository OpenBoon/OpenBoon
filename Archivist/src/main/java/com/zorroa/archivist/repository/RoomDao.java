package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.RoomBuilder;
import com.zorroa.archivist.sdk.domain.RoomUpdateBuilder;
import com.zorroa.archivist.sdk.domain.Session;

import java.util.List;

public interface RoomDao {

    Room create(RoomBuilder builder);

    Room get(long id);
    Room get(Session session);

    boolean update(Room room, RoomUpdateBuilder builder);

    boolean delete(Room room);

    String getPassword(long id);

    List<Room> getAll(Session session);

    boolean join(Room room, Session session);
}
