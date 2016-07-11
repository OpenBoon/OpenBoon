package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Room;
import com.zorroa.sdk.domain.Session;
import com.zorroa.sdk.domain.SessionAttrs;

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
