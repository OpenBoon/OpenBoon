package com.zorroa.archivist.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.elasticsearch.common.Preconditions;
import org.elasticsearch.common.collect.ImmutableSet;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;

@Repository
public class UserDaoImpl extends AbstractDao implements UserDao {

    private static final RowMapper<User> MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int row) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("pk_person"));
            user.setUsername(rs.getString("str_username"));

            String[] roles = (String[]) rs.getObject("list_roles");
            user.setRoles(ImmutableSet.<String>copyOf(roles));
            return user;
        }
    };

    @Override
    public User get(int id) {
        return jdbc.queryForObject("SELECT * FROM person WHERE pk_person=?", MAPPER, id);
    }

    @Override
    public User get(String username) {
        return jdbc.queryForObject("SELECT * FROM person WHERE str_username=?", MAPPER, username);
    }

    @Override
    public List<User> getAll() {
        return jdbc.query("SELECT * FROM person ORDER BY str_username", MAPPER);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "person " +
            "(" +
                "str_username,"+
                "str_password, " +
                "str_email,"+
                "list_roles " +
            ") VALUES (?,?,?,?)";

    @Override
    public User create(UserBuilder builder) {
        Preconditions.checkNotNull(builder.getUsername(), "The Username cannot be null");
        Preconditions.checkNotNull(builder.getPassword(), "The Password cannot be null");
        builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection connection)
                    throws SQLException {
                PreparedStatement ps =
                    connection.prepareStatement(INSERT,  new String[]{"pk_person"});
                ps.setString(1, builder.getUsername());
                ps.setString(2, builder.getPassword());
                ps.setString(3, builder.getEmail());
                ps.setObject(4, builder.getRoles().toArray(new String[]{}));
                return ps;
            }
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public String getPassword(String username) {
        return jdbc.queryForObject("SELECT str_password FROM person WHERE str_username=?", String.class, username);
    }


}
