package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.sdk.domain.Room;
import com.zorroa.archivist.sdk.domain.RoomBuilder;
import com.zorroa.archivist.sdk.domain.RoomUpdateBuilder;
import com.zorroa.archivist.sdk.domain.Session;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Preconditions;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

@Repository
public class RoomDaoImpl extends AbstractDao implements RoomDao {

    private static final RowMapper<Room> MAPPER = (rs, row) -> {
        Room room = new Room();
        room.setId(rs.getLong("pk_room"));
        room.setName(rs.getString("str_name"));
        room.setVisible(rs.getBoolean("bool_visible"));
        // FIXME: Fails when reading an array, perhaps without a default value?
//            String[] invites = (String[]) rs.getObject("list_invites");
//            room.setInviteList(ImmutableSet.<String>copyOf(invites));
        return room;
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
                    "pk_session");

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
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return get(id);
    }

    @Override
    public boolean update(Room room, RoomUpdateBuilder builder) {

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE room SET ");

        /*
         * Need to fix the invite list.
        if (builder.getInviteList() != null) {

        }
        */

        if (builder.getName() != null) {
            updates.add("str_name=?");
            values.add(builder.getName());
        }

        if (builder.getPassword() != null) {
            String salted = SecurityUtils.createPasswordHash(builder.getPassword());
            updates.add("str_password=?");
            logger.info("salted: {}", salted);
            values.add(salted);
        }

        if (values.isEmpty()) {
            return false;
        }

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_room=?");
        values.add(room.getId());

        logger.debug("Updating room {} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(Room room) {
        return jdbc.update("DELETE FROM room WHERE pk_room=?", room.getId()) == 1;
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
