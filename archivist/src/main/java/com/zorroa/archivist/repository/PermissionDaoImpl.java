package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 10/27/15.
 */
@Repository
public class PermissionDaoImpl extends AbstractDao implements PermissionDao {

    private static final String INSERT =
            JdbcUtils.insert("permission",
                    "str_name", "str_type", "str_description", "bool_immutable", "str_authority");

    @Override
    public Permission create(PermissionSpec builder, boolean immutable) {

        KeyHolder keyHolder = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement ps =
                        connection.prepareStatement(INSERT, new String[]{"pk_permission"});
                ps.setString(1, builder.getName());
                ps.setString(2, builder.getType());
                ps.setString(3, builder.getDescription() == null
                        ? String.format("%s permission", builder.getName()) : builder.getDescription());
                ps.setBoolean(4, immutable);
                ps.setString(5, builder.getType() + "::" + builder.getName());
                return ps;
            }, keyHolder);
        } catch (DuplicateKeyException e) {
            throw new DuplicateKeyException("The permission " + builder.getName() + " already exists");
        }

        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public Permission update(Permission permission) {
        String authority = String.join(Permission.JOIN, permission.getType(), permission.getName());
        jdbc.update("UPDATE permission SET str_type=?, str_name=?,str_description=?,str_authority=? WHERE pk_permission=? AND bool_immutable=?",
                permission.getType(), permission.getName(), permission.getDescription(), authority, permission.getId(), false);
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
    public int getId(String name) {
        try {
            return jdbc.queryForObject("SELECT pk_permission FROM permission WHERE str_authority=?",
                    Integer.class, name);
        } catch (EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to find permission " + name, 1);
        }
    }

    @Override
    public Permission get(String authority) {
        String[] parts = authority.split(Permission.JOIN);
        return jdbc.queryForObject("SELECT * FROM permission WHERE str_name=? AND str_type=?", MAPPER,
                parts[1], parts[0]);
    }

    @Override
    public Acl resolveAcl(Acl acl){
        if (acl == null) {
            return new Acl();
        }

        Acl result = new Acl();
        for (AclEntry entry: acl) {
            if (entry.getPermissionId() == null) {
                result.addEntry(getId(entry.permission), entry.getAccess());
            }
            else {
                result.add(entry);
            }
        }
        return result;
    }

    public static String GET_ALL =
            "SELECT * FROM permission ";

    @Override
    public List<Permission> getAll() {
        return jdbc.query(GET_ALL, MAPPER);
    }

    @Override
    public PagedList<Permission> getPaged(Pager page) {
        return getPaged(page, new PermissionFilter());
    }

    @Override
    public PagedList<Permission> getPaged(Pager page, DaoFilter filter) {
        filter.setOrderBy("str_type,str_name");
        return new PagedList(page.setTotalCount(count(filter)),
                jdbc.query(filter.getQuery(
                        GET_ALL, page),
                        MAPPER, filter.getValues(page)));
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM permission", Long.class);
    }

    @Override
    public long count(DaoFilter filter) {
        return jdbc.queryForObject(filter.getCountQuery(GET_ALL),
                Long.class, filter.getValues());
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM permission WHERE str_authority=?",
                Integer.class, name) == 1;
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
    public List<Permission> getAll(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Lists.newArrayListWithCapacity(1);
        }

        return jdbc.query("SELECT * FROM permission WHERE "
                + JdbcUtils.in("str_authority", names.size()), MAPPER, names.toArray());
    }

    @Override
    public boolean delete(Permission perm) {
        /*
         * Ensure immutable permissions cannot be deleted.
         */
        return jdbc.update("DELETE FROM permission WHERE pk_permission=? AND bool_immutable=0", perm.getId()) == 1;
    }
}
