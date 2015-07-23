package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by chambers on 7/14/15.
 */
public interface SessionDao {
    Session create(User user, String cookie);

    List<Session> getAll(Room room);

    Session get(HttpSession session);

    boolean delete(String cookie);

    boolean refreshLastRequestTime(String cookie);

    List<Session> getAll(User user);

    Session get(String cookie);

    Session get(long id);
}
