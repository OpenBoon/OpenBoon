package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.InternalPermission;
import com.zorroa.archivist.sdk.domain.Permission;
import com.zorroa.archivist.sdk.domain.PermissionBuilder;
import com.zorroa.archivist.sdk.domain.User;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
@Repository
public class PermissionDaoImpl extends AbstractDao implements PermissionDao {


    private static final String INSERT =
            JdbcUtils.insert("permission", "str_name", "str_description");

    @Override
    public Permission create(PermissionBuilder builder) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_permission"});
            ps.setString(1, builder.getName());
            ps.setString(2, builder.getDescription());
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public Permission update(Permission permission) {
        jdbc.update("UPDATE permission SET str_name=?,str_description=? WHERE pk_permission=?",
                permission.getName(), permission.getDescription(), permission.getId());
        return get(permission.getId());
    }

    private static final RowMapper<Permission> MAPPER = (rs, row) -> {
        InternalPermission p = new InternalPermission();
        p.setId(rs.getInt("pk_permission"));
        p.setName(rs.getString("str_name"));
        p.setDescription(rs.getString("str_description"));
        return p;
    };

    @Override
    public Permission get(int id) {
        return jdbc.queryForObject("SELECT * FROM permission WHERE pk_permission=?", MAPPER, id);
    }

    @Override
    public Permission get(String name) {
        return jdbc.queryForObject("SELECT * FROM permission WHERE str_name=?", MAPPER, name);
    }

    @Override
    public List<Permission> getAll() {
        return jdbc.query("SELECT * FROM permission", MAPPER);
    }

    private static final String GET_BY_USER =
            "SELECT p.* " +
            "FROM " +
                "permission p, " +
                "map_permission_to_user m " +
            "WHERE " +
                "p.pk_permission=m.pk_permission " +
            "AND " +
                "m.pk_user=?";

    @Override
    public List<Permission> getAll(User user) {
        return jdbc.query(GET_BY_USER, MAPPER, user.getId());
    }

    @Override
    public void setPermissions(User user, Collection<? extends Permission> perms) {
        deleteAll(user);
        perms.forEach(p ->
                jdbc.update("INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (?,?)",
                        p.getId(), user.getId()));
    }

    @Override
    public void setPermissions(User user, Permission... perms) {
        deleteAll(user);
        for (Permission p: perms) {
            jdbc.update("INSERT INTO map_permission_to_user (pk_permission, pk_user) VALUES (?,?)",
                    p.getId(), user.getId());
        }
    }

    private void deleteAll(User user) {
        jdbc.update("DELETE FROM map_permission_to_user WHERE pk_user=?", user.getId());
    }
}
