package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserBuilder;
import com.zorroa.archivist.domain.UserUpdateBuilder;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.Preconditions;
import org.elasticsearch.common.collect.ImmutableSet;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserDaoImpl extends AbstractDao implements UserDao {

    private static final RowMapper<User> MAPPER = new RowMapper<User>() {
        @Override
        public User mapRow(ResultSet rs, int row) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("pk_user"));
            user.setUsername(rs.getString("str_username"));
            user.setEmail(rs.getString("str_email"));
            String[] roles = (String[]) rs.getObject("list_roles");
            user.setRoles(ImmutableSet.<String>copyOf(roles));
            return user;
        }
    };

    @Override
    public User get(int id) {
        return jdbc.queryForObject("SELECT * FROM user WHERE pk_user=?", MAPPER, id);
    }

    @Override
    public User get(String username) {
        return jdbc.queryForObject("SELECT * FROM user WHERE str_username=?", MAPPER, username);
    }

    @Override
    public List<User> getAll() {
        return jdbc.query("SELECT * FROM user ORDER BY str_username", MAPPER);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "user " +
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
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT,  new String[]{"pk_user"});
            ps.setString(1, builder.getUsername());
            ps.setString(2, builder.getPassword());
            ps.setString(3, builder.getEmail());
            ps.setObject(4, builder.getRoles().toArray(new String[]{}));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public boolean update(User user, UserUpdateBuilder builder) {

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE user SET ");

        if (builder.getUsername() != null) {
            updates.add("str_username=?");
            values.add(builder.getUsername());
        }

        if (builder.getPassword() != null) {
            updates.add("str_password=?");
            values.add(SecurityUtils.createPasswordHash(builder.getPassword()));
        }

        if (builder.getEmail() != null) {
            updates.add("str_email=?");
            values.add(builder.getEmail());
        }

        if (values.isEmpty()) {
            return false;
        }

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_user=?");
        values.add(user.getId());

        logger.debug("{} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public String getPassword(String username) {
        return jdbc.queryForObject("SELECT str_password FROM user WHERE str_username=?", String.class, username);
    }
}
