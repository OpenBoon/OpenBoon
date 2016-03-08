package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.Session;
import com.zorroa.archivist.sdk.domain.SessionAttrs;
import com.zorroa.archivist.sdk.domain.User;

import java.util.List;

/**
 * Created by chambers on 7/14/15.
 */
public interface SessionDao {
    Session create(User user, String cookie);

    List<Session> getAll(Room room);

    boolean delete(String cookie);

    boolean refreshLastRequestTime(String cookie);

    List<Session> getAll(User user);

    Session get(String cookie);

    Session get(long id);

    void setAttrs(Session session, SessionAttrs attrs);
}
