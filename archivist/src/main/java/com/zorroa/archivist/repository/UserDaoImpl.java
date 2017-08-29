package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.zorroa.archivist.HttpUtils;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Collection;
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
        user.setSettings(Json.deserialize(rs.getString("json_settings"), UserSettings.class));
        user.setPermissionId(rs.getInt("pk_permission"));
        user.setHomeFolderId(rs.getInt("pk_folder"));
        return user;
    };

    @Override
    public User get(int id) {
        return jdbc.queryForObject("SELECT * FROM users WHERE pk_user=?", MAPPER, id);
    }

    @Override
    public User get(String username) {
        return jdbc.queryForObject("SELECT * FROM users WHERE str_username=? OR str_email=?",
                MAPPER, username, username);
    }

    @Override
    public User getByEmail(String email) {
        return jdbc.queryForObject("SELECT * FROM users WHERE str_email=?", MAPPER, email);
    }

    @Override
    public User getByToken(String token) {
        long expireTime = 30 * 60 * 1000;
        try {
            return jdbc.queryForObject(
                    "SELECT * FROM users WHERE str_reset_pass_token=? AND " +
                            "? - time_reset_pass < ? LIMIT 1 ",
                    MAPPER, token, System.currentTimeMillis(), expireTime);
        } catch (EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("This password change token has expired.", 1);
        }
    }

    private static final String GET_ALL =
            "SELECT * FROM users ORDER BY str_username";

    @Override
    public List<User> getAll() {
        return jdbc.query(GET_ALL, MAPPER);
    }

    @Override
    public PagedList<User> getAll(Pager page) {
        return new PagedList(page.setTotalCount(getCount()),
                jdbc.query(GET_ALL.concat(" LIMIT ? OFFSET ?"),
                        MAPPER, page.getSize(), page.getFrom()));
    }

    private static final String INSERT =
            JdbcUtils.insert("users",
                "str_username",
                "str_password",
                "str_email",
                "str_firstname",
                "str_lastname",
                "bool_enabled",
                "hmac_key",
                "json_settings",
                "str_source",
                "pk_permission",
                "pk_folder");

    @Override
    public User create(UserSpec builder, String source) {
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
            ps.setObject(7, UUID.randomUUID());
            ps.setString(8, "{}");
            ps.setString(9, source);
            ps.setInt(10, builder.getUserPermissionId());
            ps.setInt(11, builder.getHomeFolderId());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    public User create(UserSpec builder) {
        return create(builder, "local");
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM users WHERE str_username=?", Boolean.class, name);
    }

    @Override
    public boolean setSettings(User user, UserSettings settings) {
        return jdbc.update(
                "UPDATE users SET json_settings=? WHERE pk_user=?",
                Json.serializeToString(settings, "{}"), user.getId()) == 1;
    }

    @Override
    public UserSettings getSettings(int id) {
        return Json.deserialize(
                jdbc.queryForObject("SELECT json_settings FROM users WHERE pk_user=?",
                        String.class, id), UserSettings.class);
    }

    @Override
    public boolean setPassword(User user, String password) {
        return jdbc.update(
                "UPDATE users SET str_password=? WHERE pk_user=?",
                SecurityUtils.createPasswordHash(password), user.getId()) == 1;
    }

    @Override
    public String setEnablePasswordRecovery(User user) {
        String token = HttpUtils.randomString(64);
        jdbc.update(
                "UPDATE users SET str_reset_pass_token=?, time_reset_pass=? WHERE pk_user=?",
                token, System.currentTimeMillis(), user.getId());
        return token;
    }

    private static final String RESET_PASSWORD =
        "UPDATE " +
            "users " +
        "SET " +
            "str_password=?,"+
            "str_reset_pass_token=null " +
        "WHERE " +
            "pk_user=? " +
        "AND " +
            "str_reset_pass_token=?";

    @Override
    public boolean resetPassword(User user, String token, String password) {
        boolean result = jdbc.update(RESET_PASSWORD, SecurityUtils.createPasswordHash(password),
                user.getId(), token) == 1;
        return result;
    }

    @Override
    public boolean setEnabled(User user, boolean value) {
        return jdbc.update(
                "UPDATE users SET bool_enabled=? WHERE pk_user=? AND bool_enabled=?",
                value, user.getId(), !value) == 1;
    }

    private static final String UPDATE = JdbcUtils.update("users", "pk_user",
            "str_email",
            "str_firstname",
            "str_lastname");

    @Override
    public boolean update(User user, UserProfileUpdate builder) {
        return jdbc.update(UPDATE, builder.getEmail(), builder.getFirstName(),
                builder.getLastName(), user.getId()) == 1;
    }

    @Override
    public boolean delete(User user) {
        return jdbc.update("DELETE FROM users WHERE pk_user=?", user.getId()) == 1;
    }

    @Override
    public String getPassword(String username) {
        return jdbc.queryForObject("SELECT str_password FROM users WHERE (str_username=? OR str_email=?) AND bool_enabled=? AND str_source='local'",
            String.class, username, username, true);
    }

    @Override
    public String getHmacKey(String username) {
        return jdbc.queryForObject("SELECT hmac_key FROM users WHERE str_username=? AND bool_enabled=?",
                String.class, username, true);
    }

    @Override
    public boolean generateHmacKey(String username) {
        UUID key = UUID.randomUUID();
        boolean result = jdbc.update("UPDATE users SET hmac_key=? WHERE str_username=? AND bool_enabled=?",
                key, username, true) == 1;
        return result;
    }

    @Override
    public long getCount() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM users", Integer.class);
    }


    @Override
    public boolean hasPermission(User user, Permission permission) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM user_permission m WHERE m.pk_user=? AND m.pk_permission=?",
                Integer.class, user.getId(), permission.getId()) == 1;
    }

    private static final String HAS_PERM =
            "SELECT " +
                "COUNT(1) " +
            "FROM " +
                "permission p,"+
                "user_permission up " +
            "WHERE " +
                "p.pk_permission = up.pk_permission " +
            "AND " +
                "up.pk_user = ? " +
            "AND " +
                "p.str_name = ? " +
            "AND " +
                "p.str_type = ?";

    @Override
    public boolean hasPermission(User user, String type, String name) {
        return jdbc.queryForObject(HAS_PERM, Integer.class, user.getId(), name, type) == 1;
    }

    private void clearPermissions(User user) {
        /*
         * Ensure the user's immutable permissions cannot be removed.
         */
        jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND bool_immutable=?",
                user.getId(), false);
    }

    @Override
    public int setPermissions(User user, Collection<? extends Permission> perms) {
        /*
         * Does not remove immutable permissions.
         */
        clearPermissions(user);

        int result = 0;
        for (Permission p: perms) {
            if (hasPermission(user, p)) {
                continue;
            }
            jdbc.update("INSERT INTO user_permission (pk_permission, pk_user) VALUES (?,?)",
                    p.getId(), user.getId());
            result++;
        }
        return result;
    }

    @Override
    public boolean addPermission(User user, Permission perm, boolean immutable) {
        if (hasPermission(user, perm)) {
            return false;
        }
        return jdbc.update("INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (?,?,?)",
                perm.getId(), user.getId(), immutable) == 1;
    }

    @Override
    public boolean removePermission(User user, Permission perm) {
        return jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND pk_permission=? AND bool_immutable=0",
                user.getId(), perm.getId()) == 1;
    }
}
