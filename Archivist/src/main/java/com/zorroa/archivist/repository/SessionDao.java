package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;

import java.util.List;

/**
 * Created by chambers on 7/14/15.
 */
public interface SessionDao {
    void create(User user, String sessionId);

    boolean delete(String sessionId);

    boolean refreshLastRequestTime(String sessionId);

    List<Session> getAll(User user);
}
