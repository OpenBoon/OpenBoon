package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.Session;
import com.zorroa.archivist.sdk.domain.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import javax.servlet.http.HttpSession;
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
                    "time_last_request");

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
            return ps;
        }, keyHolder);

        long id = keyHolder.getKey().longValue();

        // Create the session to room mapping with a null room. The room gets
        // joined and updated at a later point.
        jdbc.update("INSERT INTO map_session_to_room (pk_session, pk_room) VALUES (?,?)", id, null);

        Session s = new Session();
        s.setId(id);
        s.setUserId(user.getId());
        s.setUsername(user.getUsername());
        s.setRefreshTime(time);
        s.setCookieId(cookieId);
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
    public List<Session> getAll(Room room) {
        return jdbc.query(GET + " INNER JOIN map_session_to_room m ON (m.pk_session=session.pk_session) " +
                        "WHERE m.pk_room=?",
                MAPPER, room.getId());
    }

    @Override
    public Session get(String cookie) {
        return jdbc.queryForObject(GET + " WHERE session.cookie_id=?", MAPPER, cookie);
    }

    @Override
    public Session get(long id) {
        return jdbc.queryForObject(GET + " WHERE session.pk_session=?", MAPPER, id);
    }
}
