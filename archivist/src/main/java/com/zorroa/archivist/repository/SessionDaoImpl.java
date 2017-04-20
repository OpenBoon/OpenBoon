package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Session;
import com.zorroa.sdk.domain.SessionAttrs;
import com.zorroa.sdk.util.Json;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 7/14/15.
 */
@Repository
public class SessionDaoImpl extends AbstractDao implements SessionDao {

    private static final String INSERT =
            JdbcUtils.insert("session",
                    "pk_user",
                    "cookie_id",
                    "bool_expired",
                    "time_last_request",
                    "json_attrs");

    @Override
    public Session create(User user, String cookieId) {
        long time = System.currentTimeMillis();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_session"});
            ps.setInt(1, user.getId());
            ps.setString(2, cookieId);
            ps.setBoolean(3, false);
            ps.setLong(4, time);
            ps.setString(5, "{}");
            return ps;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();

        Session s = new Session();
        s.setId(id);
        s.setUserId(user.getId());
        s.setUsername(user.getUsername());
        s.setRefreshTime(time);
        s.setCookieId(cookieId);
        s.setAttrs(new SessionAttrs());
        return s;
    }

    @Override
    public boolean delete(String cookieId) {
        return jdbc.update("DELETE FROM session WHERE cookie_id=?", cookieId) == 1;
    }

    @Override
    public boolean refreshLastRequestTime(String cookieId) {
        return jdbc.update("UPDATE session SET time_last_request=? WHERE cookie_id=?",
                System.currentTimeMillis(), cookieId) == 1;
    }

    static final RowMapper<Session> MAPPER = (rs, row) -> {
        Session session = new Session();
        session.setId(rs.getLong("pk_session"));
        session.setCookieId(rs.getString("cookie_id"));
        session.setRefreshTime(rs.getLong("time_last_request"));
        session.setUsername(rs.getString("str_username"));
        session.setUserId(rs.getInt("pk_user"));
        session.setAttrs(Json.deserialize(rs.getString("json_attrs"), SessionAttrs.class));
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

    @Override
    public Session get(String cookie) {
        try {
            return jdbc.queryForObject(GET + " WHERE session.cookie_id=?", MAPPER, cookie);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public Session get(long id) {
        return jdbc.queryForObject(GET + " WHERE session.pk_session=?", MAPPER, id);
    }

    @Override
    public void setAttrs(Session session, SessionAttrs attrs) {
        jdbc.update("UPDATE session SET json_attrs=? WHERE pk_session=?",
                Json.serializeToString(attrs), session.getId());
    }
}
