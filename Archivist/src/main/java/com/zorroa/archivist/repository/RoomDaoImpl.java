package com.zorroa.archivist.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.elasticsearch.common.Preconditions;
import org.elasticsearch.common.collect.ImmutableSet;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.Room;
import com.zorroa.archivist.domain.RoomBuilder;

@Repository
public class RoomDaoImpl extends AbstractDao implements RoomDao {


    private static final RowMapper<Room> MAPPER = new RowMapper<Room>() {
        @Override
        public Room mapRow(ResultSet rs, int row) throws SQLException {
            Room room = new Room();
            room.setId(rs.getLong("pk_room"));
            room.setName(rs.getString("str_name"));
            room.setVisible(rs.getBoolean("bool_visible"));

            String[] invites = (String[]) rs.getObject("list_invites");
            room.setInviteList(ImmutableSet.<String>copyOf(invites));
            return room;
        }
    };

    @Override
    public Room get(long id) {
        return jdbc.queryForObject("SELECT * FROM room WHERE pk_room=?", MAPPER, id);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "room " +
            "(" +
                "str_name,"+
                "str_session, " +
                "str_password,"+
                "bool_visible, "+
                "list_invites " +
            ") VALUES (?,?,?,?,?)";

    @Override
    public Room create(RoomBuilder builder) {
        Preconditions.checkNotNull(builder.getName(), "The room name cannot be null");
        if (builder.getPassword()!=null) {
            builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection connection)
                    throws SQLException {
                PreparedStatement ps =
                    connection.prepareStatement(INSERT,  new String[]{"pk_person"});
                ps.setString(1, builder.getName());
                ps.setString(2, builder.getSession());
                ps.setString(3, builder.getPassword());
                ps.setBoolean(4, builder.isVisible());
                if (builder.getInviteList() ==null) {
                     ps.setObject(5, new String[] {});
                }
                else {
                     ps.setObject(5, builder.getInviteList().toArray(new String[]{}));
                }
                return ps;
            }
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return get(id);
    }


    @Override
    public String getPassword(long id) {
        return jdbc.queryForObject("SELECT str_password FROM room WHERE pk_room=?", String.class, id);
    }


}
