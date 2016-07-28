package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserSpec;
import com.zorroa.archivist.domain.UserUpdate;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Room;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

@Repository
public class UserDaoImpl extends AbstractDao implements UserDao {

    private static final RowMapper<User> MAPPER = (rs, row) -> {
        User user = new User();
        user.setId(rs.getInt("pk_user"));
        user.setUsername(rs.getString("str_username"));
        user.setEmail(rs.getString("str_email"));
        user.setFirstName(rs.getString("str_firstname"));
        user.setLastName(rs.getString("str_lastname"));
        user.setEnabled(rs.getBoolean("bool_enabled"));
        return user;
    };

    @Override
    public User get(int id) {
        return jdbc.queryForObject("SELECT * FROM user WHERE pk_user=?", MAPPER, id);
    }

    @Override
    public User get(String username) {
        return jdbc.queryForObject("SELECT * FROM user WHERE str_username=?", MAPPER, username);
    }

    private static final String GET_ALL =
            "SELECT * FROM user ORDER BY str_username";

    @Override
    public List<User> getAll() {
        return jdbc.query(GET_ALL, MAPPER);
    }

    @Override
    public PagedList<User> getAll(Paging page) {
        return new PagedList(page.setTotalCount(getCount()),
                jdbc.query(GET_ALL.concat(" LIMIT ? OFFSET ?"),
                        MAPPER, page.getSize(), page.getFrom()));
    }

    private static final String INSERT =
            "INSERT INTO " +
                "user " +
            "(" +
                "str_username, "+
                "str_password, " +
                "str_email, "+
                "str_firstname, " +
                "str_lastname, " +
                "bool_enabled " +
            ") VALUES (?,?,?,?,?,?)";

    @Override
    public User create(UserSpec builder) {
        Preconditions.checkNotNull(builder.getUsername(), "The Username cannot be null");
        Preconditions.checkNotNull(builder.getPassword(), "The Password cannot be null");
        builder.setPassword(SecurityUtils.createPasswordHash(builder.getPassword()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_user"});
            ps.setString(1, builder.getUsername());
            ps.setString(2, builder.getPassword());
            ps.setString(3, builder.getEmail());
            ps.setString(4, builder.getFirstName());
            ps.setString(5, builder.getLastName());
            ps.setBoolean(6, true);
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM user WHERE str_username=?", Boolean.class, name);
    }

    @Override
    public boolean setPassword(User user, String password) {
        return jdbc.update(
                "UPDATE user SET str_password=? WHERE pk_user=?",
                SecurityUtils.createPasswordHash(password), user.getId()) == 1;
    }

    @Override
    public boolean setEnabled(User user, boolean value) {
        return jdbc.update(
                "UPDATE user SET bool_enabled=? WHERE pk_user=? AND bool_enabled=?",
                value, user.getId(), !value) == 1;
    }

    private static final String UPDATE = JdbcUtils.update("user", "pk_user",
            "str_email",
            "str_firstname",
            "str_lastname");

    @Override
    public boolean update(User user, UserUpdate builder) {
        logger.info("{}", builder.getEmail());
        return jdbc.update(UPDATE, builder.getEmail(), builder.getFirstName(),
                builder.getLastName(), user.getId()) == 1;
    }

    @Override
    public String getPassword(String username) {
        return jdbc.queryForObject("SELECT str_password FROM user WHERE str_username=? AND bool_enabled=?",
            String.class, username, true);
    }

    @Override
    public String getHmacKey(String username) {
        return jdbc.queryForObject("SELECT hmac_key FROM user WHERE str_username=? AND bool_enabled=?",
                String.class, username, true);
    }

    @Override
    public boolean generateHmacKey(String username) {
        UUID key = UUID.randomUUID();
        logger.info("generated key {} for {}", key, username);
        boolean result = jdbc.update("UPDATE user SET hmac_key=? WHERE str_username=? AND bool_enabled=?",
                key, username, true) == 1;
        return result;
    }

    private static final String GET_ALL_BY_ROOM =
        "SELECT " +
            "user.* " +
        "FROM " +
            "user,session,map_session_to_room m " +
        "WHERE " +
            "session.pk_session = m.pk_session " +
        "AND " +
            "m.pk_room = ? " +
        "AND " +
            "session.pk_user = user.pk_user " +
        "ORDER BY "+
            "user.str_username ASC";

    @Override
    public List<User> getAll(Room room) {
        return jdbc.query(GET_ALL_BY_ROOM, MAPPER, room.getId());
    }

    private static final String GET_ALL_WITH_SESSION =
            "SELECT " +
                "DITINCT user.* " +
            "FROM " +
                "user,session " +
            "WHERE " +
                "session.pk_user = user.pk_user ";

    @Override
    public List<User> getAllWithSession() {
        return jdbc.query(GET_ALL_WITH_SESSION, MAPPER);
    }

    @Override
    public long getCount() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM user", Integer.class);
    }

}
