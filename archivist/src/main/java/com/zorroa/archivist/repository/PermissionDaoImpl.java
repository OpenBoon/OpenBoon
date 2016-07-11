package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.domain.User;
import com.zorroa.sdk.domain.Permission;
import com.zorroa.sdk.domain.PermissionBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
@Repository
public class PermissionDaoImpl extends AbstractDao implements PermissionDao {

    private static final String INSERT =
            JdbcUtils.insert("permission", "str_name", "str_type", "str_description", "bool_immutable");

    @Override
    public Permission create(PermissionBuilder builder, boolean immutable) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_permission"});
            ps.setString(1, builder.getName());
            ps.setString(2, builder.getType());
            ps.setString(3, builder.getDescription() == null
                    ? String.format("%s permission", builder.getName()) : builder.getDescription());
            ps.setBoolean(4, immutable);
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public Permission update(Permission permission) {
        jdbc.update("UPDATE permission SET str_type=?, str_name=?,str_description=? WHERE pk_permission=? AND bool_immutable=?",
                permission.getType(), permission.getName(), permission.getDescription(), permission.getId(), false);
        return get(permission.getId());
    }

    @Override
    public boolean updateUserPermission(String oldName, String newName) {
        return jdbc.update("UPDATE permission SET str_name=? WHERE str_type='user' AND str_name=? AND bool_immutable=?",
                newName, oldName, true) == 1;
    }

    private static final RowMapper<Permission> MAPPER = (rs, row) -> {
        InternalPermission p = new InternalPermission();
        p.setId(rs.getInt("pk_permission"));
        p.setName(rs.getString("str_name"));
        p.setType(rs.getString("str_type"));
        p.setDescription(rs.getString("str_description"));
        p.setImmutable(rs.getBoolean("bool_immutable"));
        return p;
    };

    @Override
    public Permission get(int id) {
        return jdbc.queryForObject("SELECT * FROM permission WHERE pk_permission=?", MAPPER, id);
    }

    @Override
    public Permission get(String authority) {
        String[] parts = authority.split("::");
        return jdbc.queryForObject("SELECT * FROM permission WHERE str_type=? AND str_name=?", MAPPER, parts);
    }

    @Override
    public List<Permission> getAll() {
        /**
         * This function does not return user/internal perms, just the perm that can be
         * manually assigned.
         */
        return jdbc.query("SELECT * FROM permission WHERE str_type NOT IN ('user', 'internal')", MAPPER);
    }

    private static final String GET_BY_USER =
            "SELECT p.* " +
            "FROM " +
                "permission p, " +
                "user_permission m " +
            "WHERE " +
                "p.pk_permission=m.pk_permission " +
            "AND " +
                "m.pk_user=?";

    @Override
    public List<Permission> getAll(User user) {
        return jdbc.query(GET_BY_USER, MAPPER, user.getId());
    }

    @Override
    public List<Permission> getAll(String type) {
        return jdbc.query("SELECT * FROM permission WHERE str_type=?", MAPPER, type);
    }

    @Override
    public Permission get(String type, String name) {
        return jdbc.queryForObject("SELECT * FROM permission WHERE str_name=? AND str_type=?", MAPPER, name, type);
    }

    @Override
    public List<Permission> getAll(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            return Lists.newArrayListWithCapacity(1);
        }
        return jdbc.query("SELECT * FROM permission WHERE "
                + JdbcUtils.in("pk_permission", ids.length), MAPPER, ids);
    }

    @Override
    public void setOnUser(User user, Collection<? extends Permission> perms) {
        removeAllPermissions(user);
        for (Permission p: perms) {
            /*
             * Don't re-assign user permissions, since those cannot be removed.
             */
            if (hasPermission(user, p) || p.getType().equals("user")) {
                continue;
            }
            jdbc.update("INSERT INTO user_permission (pk_permission, pk_user) VALUES (?,?)",
                    p.getId(), user.getId());
        }
    }

    @Override
    public void setOnUser(User user, Permission ... perms) {
        setOnUser(user, Arrays.asList(perms));
    }

    @Override
    public boolean assign(User user, Permission perm, boolean immutable) {
        if (hasPermission(user, perm)) {
            return false;
        }
        return jdbc.update("INSERT INTO user_permission (pk_permission, pk_user, bool_immutable) VALUES (?,?,?)",
                perm.getId(), user.getId(), immutable) == 1;
    }

    @Override
    public boolean delete(Permission perm) {
        /*
         * Ensure immutable permissions cannot be deleted.
         */
        return jdbc.update("DELETE FROM permission WHERE pk_permission=? AND bool_immutable=0", perm.getId()) == 1;
    }

    @Override
    public boolean delete(User user) {
        return jdbc.update("DELETE FROM permission WHERE str_type='user' AND str_name=?", user.getUsername()) == 1;
    }

    private void removeAllPermissions(User user) {
        /*
         * Ensure the user's immutable permissions cannot be removed.
         */
        jdbc.update("DELETE FROM user_permission WHERE pk_user=? AND bool_immutable=0", user.getId());
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
}
