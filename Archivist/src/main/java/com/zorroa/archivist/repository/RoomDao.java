package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

public interface RoomDao {

    Room get(String id);

    Room create(RoomBuilder builder);

    String getPassword(String id);

}
