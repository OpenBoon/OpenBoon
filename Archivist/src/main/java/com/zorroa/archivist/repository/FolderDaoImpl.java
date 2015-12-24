package com.zorroa.archivist.repository;

import com.drew.lang.annotations.Nullable;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;

@Repository
public class FolderDaoImpl extends AbstractDao implements FolderDao {

    private final RowMapper<Folder> MAPPER = (rs, row) -> {
        Folder folder = new Folder();
        folder.setId(rs.getInt("pk_folder"));
        folder.setName(rs.getString("str_name"));
        folder.setUserCreated(rs.getInt("user_created"));
        folder.setUserModified(rs.getInt("user_modified"));
        folder.setRecursive(rs.getBoolean("bool_recursive"));
        folder.setTimeCreated(rs.getLong("time_created"));
        folder.setTimeModified(rs.getLong("time_modified"));

        Object parent = rs.getObject("pk_parent");
        if (parent != null) {
            folder.setParentId((Integer) parent);
        }

        String search = rs.getString("json_search");
        if (search != null) {
            folder.setSearch(Json.deserialize(search, AssetSearch.class));
        }

        /*
         * Might turn into an issue but we have lots of caching around folders
         */
        folder.setAcl(getAcl(folder));
        return folder;
    };

    private static final String GET =
            "SELECT " +
                "* " +
            "FROM " +
                "folder ";
    @Override
    public Folder get(int id) {
        return jdbc.queryForObject(appendReadAccess(GET + " WHERE pk_folder=?"), MAPPER, appendAclArgs(id));
    }

    @Override
    public Folder get(int parent, String name) {
        return jdbc.queryForObject(
                appendReadAccess(GET + " WHERE pk_parent=? and str_name=?"), MAPPER,
                appendAclArgs(parent, name));
    }

    @Override
    public Folder get(Folder parent, String name) {
        return get(parent.getId(), name);
    }

    @Override
    public List<Folder> getAll(Collection<Integer> ids) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(GET);
        sb.append(" WHERE ");
        sb.append(JdbcUtils.in("pk_folder", ids.size()));
        return jdbc.query(sb.toString(), MAPPER, ids.toArray());
    }

    @Override
    public List<Folder> getChildren(int parentId) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(GET);
        sb.append(" WHERE pk_parent=?");
        return jdbc.query(appendReadAccess(sb.toString()), MAPPER, appendAclArgs(parentId));
    }

    @Override
    public List<Folder> getChildrenInsecure(int parentId) {
        StringBuilder sb = new StringBuilder(512);
        sb.append(GET);
        sb.append(" WHERE pk_parent=?");
        return jdbc.query(sb.toString(), MAPPER, parentId);
    }

    @Override
    public List<Folder> getChildren(Folder folder) {
        return getChildren(folder.getId());
    }

    @Override
    public boolean exists(int parentId, String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder WHERE pk_parent=? AND str_name=?",
                Integer.class, parentId, name) == 1;
    }

    @Override
    public boolean exists(Folder parent, String name) {
        return exists(parent.getId(), name);
    }

    private static final String INSERT =
            JdbcUtils.insert("folder",
                    "pk_parent",
                    "str_name",
                    "user_created",
                    "time_created",
                    "user_modified",
                    "time_modified",
                    "json_search");

    @Override
    public Folder create(FolderBuilder builder) {
        long time = System.currentTimeMillis();
        int user = SecurityUtils.getUser().getId();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_folder"});
            ps.setInt(1, builder.getParentId());
            ps.setString(2, builder.getName());
            ps.setInt(3, user);
            ps.setLong(4, time);
            ps.setInt(5, user);
            ps.setLong(6, time);
            ps.setString(7, Json.serializeToString(builder.getSearch(), null));
            return ps;
        }, keyHolder);

        Folder folder = new Folder();
        folder.setId(keyHolder.getKey().intValue());
        folder.setParentId(builder.getParentId());
        folder.setName(builder.getName());
        folder.setSearch(builder.getSearch());
        folder.setUserCreated(user);
        folder.setAcl(builder.getAcl());
        return folder;
    }

    @Override
    public boolean update(Folder folder, FolderBuilder builder) {

        List<Object> values = Lists.newArrayList();
        List<String> sets = Lists.newArrayList();

        if (builder.getName() != null) {
            sets.add("str_name=?");
            values.add(builder.getName());
        }

        sets.add("pk_parent=?");
        values.add(builder.getParentId());

        sets.add("json_search=?");
        values.add(Json.serializeToString(builder.getSearch(), null));

        values.add(folder.getId());

        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE folder SET ");
        sb.append(String.join(",", sets));
        sb.append(" WHERE pk_folder=? ");

        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(Folder folder) {
        return jdbc.update("DELETE FROM folder WHERE pk_folder=?", folder.getId()) ==1;
    }

    @Override
    public boolean hasAccess(Folder folder, Access access) {
        return jdbc.queryForObject(appendAccess("SELECT COUNT(1) FROM folder WHERE pk_folder=?", access),
                Integer.class, appendAclArgs(folder.getId())) > 0;
    }

    @Override
    public void setAcl(Folder folder, @Nullable Acl acl) {
        jdbc.update("DELETE FROM folder_acl WHERE pk_folder=?", folder.getId());
        if (acl == null || acl.isEmpty()) {
            return;
        }
        for (AclEntry entry: acl) {
            jdbc.update("INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (?,?,?)",
                    entry.getPermissionId(), folder.getId(), entry.getAccess());
        }
    }

    @Override
    public Acl getAcl(Folder folder) {
        Acl result = new Acl();
        jdbc.query("SELECT * FROM  folder_acl WHERE pk_folder=?", rs -> {
            result.add(new AclEntry(rs.getInt("pk_permission"), rs.getInt("int_access")));
        }, folder.getId());
        return result;
    }

    /**
     * Append the permissions check to the given query.
     *
     * @param query
     * @return
     */
    private String appendAccess(String query, Access access) {
        if (SecurityUtils.hasPermission("group::superuser")) {
            return query;
        }

        StringBuilder sb = new StringBuilder(query.length() + 256);
        sb.append(query);
        if (query.contains("WHERE")) {
            sb.append(" AND ");
        }
        else {
            sb.append(" WHERE ");
        }
        sb.append("((");
        sb.append("SELECT COUNT(1) FROM folder_acl WHERE folder_acl.pk_folder=folder.pk_folder AND ");
        sb.append(JdbcUtils.in("folder_acl.pk_permission", SecurityUtils.getPermissionIds().size()));
        sb.append(" AND BITAND(");
        sb.append(access.getValue());
        sb.append(",int_access) = " + access.getValue() + ") > 0 OR (");
        sb.append("SELECT COUNT(1) FROM folder_acl WHERE folder_acl.pk_folder=folder.pk_folder) = 0)");
        return sb.toString();
    }

    /**
     * Append the permissions check to the given query.
     *
     * @param query
     * @return
     */
    private String appendWriteAccess(String query) {
        return appendAccess(query, Access.Write);
    }

    private String appendReadAccess(String query) {
        return appendAccess(query, Access.Read);
    }

    public Object[] appendAclArgs(Object ... args) {
        if (SecurityUtils.hasPermission("group::superuser")) {
            return args;
        }

        List<Object> result = Lists.newArrayListWithCapacity(args.length + SecurityUtils.getPermissionIds().size());
        for (Object a: args) {
            result.add(a);
        }
        result.addAll(SecurityUtils.getPermissionIds());
        return result.toArray();
    }
}
