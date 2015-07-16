package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Session;
import com.zorroa.archivist.domain.User;

import org.springframework.jdbc.core.RowMapper;
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

    static final RowMapper<Session> MAPPER = (rs, row) -> {
        Session session = new Session();
        session.setId(rs.getLong("pk_session"));
        session.setSessionId(rs.getString("session_id"));
        session.setRefreshTime(rs.getLong("time_last_request"));
        session.setUsername(rs.getString("str_username"));
        session.setUserId(rs.getInt("pk_user"));
        return session;
    };

    static final String GET =
        "SELECT " +
            "session.*," +
            "user.str_username " +
        "FROM " +
            "session INNER JOIN user ON (session.pk_user = user.pk_user) ";

    @Override
    public List<Session> getAll(User user) {
        return jdbc.query(GET + " WHERE user.pk_user=?", MAPPER, user.getId());
    }
}
