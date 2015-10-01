package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;
import com.zorroa.archivist.domain.Session;
import org.elasticsearch.common.Preconditions;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
public class RoomDaoImpl extends AbstractDao implements RoomDao {


    private static final RowMapper<Room> MAPPER = new RowMapper<Room>() {
        @Override
        public Room mapRow(ResultSet rs, int row) throws SQLException {
            Room room = new Room();
            room.setId(rs.getLong("pk_room"));
            room.setName(rs.getString("str_name"));
            room.setVisible(rs.getBoolean("bool_visible"));
            room.setFolderId(rs.getString("str_folderId"));
            // FIXME: Fails when reading an array, perhaps without a default value?
//            String[] invites = (String[]) rs.getObject("list_invites");
//            room.setInviteList(ImmutableSet.<String>copyOf(invites));
            return room;
        }
    };

    @Override
    public Room get(long id) {
        return jdbc.queryForObject("SELECT * FROM room WHERE pk_room=?", MAPPER, id);
    }

    @Override
    public Room get(Session session) {
        try {
            return jdbc.queryForObject("SELECT room.* FROM room, map_session_to_room m " +
                    "WHERE room.pk_room=m.pk_room AND m.pk_session=?", MAPPER, session.getId());
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static final String INSERT =
            JdbcUtils.insert("room",
                    "str_name",
                    "str_password",
                    "bool_visible",
                    "list_invites",
                    "pk_session",
                    "str_folderId");

    @Override
    public Room create(RoomBuilder builder) {
        Preconditions.checkNotNull(builder.getName(), "The room name cannot be null");
        if (builder.getPassword()!= null) {
            builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_room"});
            ps.setString(1, builder.getName());
            ps.setString(2, builder.getPassword());
            ps.setBoolean(3, builder.isVisible());
            if (builder.getInviteList() == null) {
                ps.setObject(4, new String[]{});
            }
            else {
                ps.setObject(4, builder.getInviteList().toArray(new String[]{}));
            }

            if (builder.getSessionId() == null) {
                ps.setNull(5, Types.BIGINT);
            }
            else {
                ps.setLong(5, builder.getSessionId());
            }
            if (builder.getFolderId() == null) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, builder.getFolderId());
            }
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return get(id);
    }


    @Override
    public String getPassword(long id) {
        return jdbc.queryForObject("SELECT str_password FROM room WHERE pk_room=?", String.class, id);
    }

    @Override
    public List<Room> getAll(Session session) {
        /*
         * Should return all rooms and our own session room.
         */
        return jdbc.query("SELECT * FROM room WHERE (bool_visible='t' OR pk_session=?)", MAPPER, session.getId());
    }

    @Override
    public boolean join(Room room, Session session) {
        // TODO: query is not letting me add AND pk_room!=new room.
        int result = jdbc.update("UPDATE map_session_to_room SET pk_room=? WHERE pk_session=?",
                room.getId(), session.getId());
        return result == 1;
    }
}
