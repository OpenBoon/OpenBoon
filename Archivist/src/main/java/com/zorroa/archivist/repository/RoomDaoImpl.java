package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Preconditions;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;

@Repository
public class RoomDaoImpl extends AbstractDao implements RoomDao {

    private static final RowMapper<Room> MAPPER = (rs, row) -> {
        Room room = new Room();
        room.setId(rs.getLong("pk_room"));
        room.setName(rs.getString("str_name"));
        room.setVisible(rs.getBoolean("bool_visible"));
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
                    "json_search",
                    "json_selection");

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
            ps.setString(4, Json.serializeToString(builder.getSearch(), null));
            ps.setString(5, Json.serializeToString(builder.getSelection()));
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
    public List<Room> getAll() {
        return jdbc.query("SELECT * FROM room WHERE bool_visible=1", MAPPER);
    }

    @Override
    public boolean join(Room room, Session session) {

        if (jdbc.update("UPDATE map_session_to_room SET pk_room=? WHERE pk_session=?",
                room.getId(), session.getId()) == 1) {
            return true;
        }
        jdbc.update("INSERT INTO map_session_to_room (pk_room, pk_session) VALUES (?,?)",
                room.getId(), session.getId());
        return true;
    }

    @Override
    public boolean leave(Room room, Session session) {
        return jdbc.update("DELETE FROM map_session_to_room WHERE pk_room=? AND pk_session=?",
                room.getId(), session.getId()) == 1;
    }

    @Override
    public boolean isInRoom(Room room, Session session) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM map_session_to_room WHERE pk_room=? AND pk_session=?",
                Integer.class, room.getId(), session.getId()) == 1;
    }

    private static final String UPDATE_SELECTION =
            "UPDATE " +
                "room " +
            "SET " +
                "json_selection=?,"+
                "int_version=int_version+1 " +
            "WHERE " +
                "pk_room=?";

    @Override
    public Set<String> getSelection(Room room) {
        return Json.deserialize(jdbc.queryForObject(
                "SELECT json_selection FROM room WHERE pk_room=?", String.class, room.getId()), SET_TYPE_REFERENCE);
    }

    @Override
    public int setSelection(Room room, Set<String> selection) {
        jdbc.update(UPDATE_SELECTION, Json.serializeToString(selection), room.getId());
        return jdbc.queryForObject("SELECT int_version FROM room WHERE pk_room=?", Integer.class, room.getId());
    }

    private static final String UPDATE_SEARCH =
            "UPDATE " +
                "room " +
            "SET " +
                "json_search=?,"+
                "json_selection='[]',"+
                "int_version=int_version+1 " +
            "WHERE " +
                "pk_room=?";

    @Override
    public AssetSearch getSearch(Room room) {
        return Json.deserialize(jdbc.queryForObject(
                "SELECT json_search FROM room WHERE pk_room=?", String.class, room.getId()), AssetSearch.class);
    }

    @Override
    public int setSearch(Room room, AssetSearch search) {
        jdbc.update(UPDATE_SEARCH, Json.serializeToString(search), room.getId());
        return jdbc.queryForObject("SELECT int_version FROM room WHERE pk_room=?", Integer.class, room.getId());
    }

    private static final String GET_SHARED_STATE =
            "SELECT " +
                "json_search,"+
                "json_selection, " +
                "int_version " +
            "FROM " +
                "room " +
            "WHERE " +
                "pk_room=?";
    @Override
    public SharedRoomState getSharedState(Room room) {
        SharedRoomState result = new SharedRoomState();
        jdbc.query(GET_SHARED_STATE, rs -> {
            // AssetSearch can be null
            String assetSearchJson = rs.getString(1);
            if (assetSearchJson != null) {
                result.setSearch(Json.deserialize(assetSearchJson, AssetSearch.class));
            }
            // Empty selection is actually [], but check just to be safe
            String selectionJson = rs.getString(2);
            if (selectionJson != null) {
                result.setSelection(Json.deserialize(selectionJson, SET_TYPE_REFERENCE));
            }
            result.setVersion(rs.getInt(3));
        }, room.getId());
        return result;
    }

    private static final TypeReference<Set<String>> SET_TYPE_REFERENCE = new TypeReference<Set<String>>() {};
}
