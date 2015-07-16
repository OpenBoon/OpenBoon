package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.User;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by chambers on 7/14/15.
 */
@Repository
public class SessionDaoImpl extends AbstractDao implements SessionDao {

    private static final String INSERT =
            JdbcUtils.insert("session",
                    "pk_user",
                    "session_id",
                    "bool_expired",
                    "time_last_request");

    @Override
    public void create(User user, String sessionId) {
        jdbc.update(INSERT, user.getId(), sessionId, false, System.currentTimeMillis());
    }

    @Override
    public boolean delete(String sessionId) {
        return jdbc.update("DELETE FROM session WHERE session_id=?", sessionId) == 1;
    }

    @Override
    public boolean refreshLastRequestTime(String sessionId) {
        return jdbc.update("UPDATE session SET time_last_request=? WHERE session_id=?",
                System.currentTimeMillis(), sessionId) == 1;
    }

    @Override
    public List<String> getAll(User user) {
        return jdbc.queryForList("SELECT session_id FROM session WHERE pk_user=?", String.class, user.getId());
    }
}
