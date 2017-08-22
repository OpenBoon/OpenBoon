package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.client.exception.ArchivistWriteException;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class FolderDaoImpl extends AbstractDao implements FolderDao {

    @Autowired
    UserDaoCache userDaoCache;


    private final RowMapper<Folder> MAPPER = (rs, row) -> {
        Folder folder = new Folder();
        folder.setId(rs.getInt("pk_folder"));
        folder.setName(rs.getString("str_name"));
        folder.setUser(userDaoCache.getUser(rs.getInt("user_created")));
        folder.setRecursive(rs.getBoolean("bool_recursive"));
        folder.setTimeCreated(rs.getLong("time_created"));
        folder.setTimeModified(rs.getLong("time_modified"));
        folder.setDyhiRoot(rs.getBoolean("bool_dyhi_root"));
        folder.setChildCount(rs.getInt("int_child_count"));
        folder.setTaxonomyRoot(rs.getBoolean("bool_tax_root"));

        Object parent = rs.getObject("pk_parent");
        if (parent != null) {
            folder.setParentId((Integer) parent);
        }

        Object dyhi = rs.getObject("pk_dyhi");
        if (dyhi != null) {
            folder.setDyhiId((Integer)dyhi);
        }

        String dyhiField = rs.getString("str_dyhi_field");

        String attrs = rs.getString("json_attrs");
        if (attrs != null) {
            folder.setAttrs(Json.deserialize(attrs, Map.class));
        }

        String search = rs.getString("json_search");
        if (search == null && dyhiField != null) {
            search = "{}";
        }

        if (search != null) {
            folder.setSearch(Json.deserialize(search, AssetSearch.class));
            /**
             * The dyhi field is added to the search on the fly, not baked in.
             */
            if (dyhiField != null) {
                folder.getSearch().addToFilter().addToExists(dyhiField);
            }
        }

        /*
         * Might turn into an issue but we have lots of caching around folders
         */
        folder.setAcl(getAcl(folder.getId()));
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
    public Folder get(int parent, String name, boolean ignorePerms) {
        try {
            if (ignorePerms) {
                return jdbc.queryForObject(GET + " WHERE pk_parent=? and str_name=?", MAPPER, parent, name);
            }
            else {
                return jdbc.queryForObject(
                        appendReadAccess(GET + " WHERE pk_parent=? and str_name=?"), MAPPER,
                        appendAclArgs(parent, name));
            }
        } catch (EmptyResultDataAccessException e ) {
            throw new EmptyResultDataAccessException(String.format("Failed to find folder, parent: %s name: %s",
                    parent, name), 1);
        }
    }

    @Override
    public Folder get(Folder parent, String name) {
        return get(parent.getId(), name, false);
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
    public List<Integer> getAllIds(DyHierarchy dyhi) {
        return jdbc.queryForList("SELECT pk_folder FROM folder WHERE pk_dyhi=?", Integer.class, dyhi.getId());
    }

    @Override
    public boolean exists(int parentId, String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder WHERE pk_parent=? AND str_name=?",
                Integer.class, parentId, name) == 1;
    }

    @Override
    public int count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder", Integer.class);
    }

    @Override
    public int count(DyHierarchy d) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder WHERE pk_dyhi=?", Integer.class, d.getId());
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
                    "bool_recursive",
                    "user_modified",
                    "time_modified",
                    "json_search",
                    "pk_dyhi",
                    "json_attrs");

    @Override
    public Folder create(FolderSpec spec) {
        long time = System.currentTimeMillis();

        if (spec.getUserId() == null) {
            int user = SecurityUtils.getUser().getId();
            spec.setUserId(user);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_folder"});
            ps.setInt(1, spec.getParentId() == null ?  Folder.ROOT_ID : spec.getParentId());
            ps.setString(2, spec.getName());
            ps.setInt(3, spec.getUserId());
            ps.setLong(4, time);
            ps.setBoolean(5, spec.isRecursive());
            ps.setInt(6, spec.getUserId());
            ps.setLong(7, time);
            ps.setString(8, Json.serializeToString(spec.getSearch(), null));
            ps.setObject(9, spec.getDyhiId());
            ps.setString(10, Json.serializeToString(spec.getAttrs(), "{}"));
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        return getAfterCreate(id);
    }

    private static final String RESTORE =
            JdbcUtils.insert("folder",
                    "pk_folder",
                    "pk_parent",
                    "str_name",
                    "user_created",
                    "time_created",
                    "bool_recursive",
                    "user_modified",
                    "time_modified",
                    "json_search",
                    "json_attrs");
    @Override
    public Folder create(TrashedFolder spec) {
        long time = System.currentTimeMillis();

        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(RESTORE);
            ps.setInt(1, spec.getFolderId());
            ps.setInt(2, spec.getParentId() == null ?  Folder.ROOT_ID : spec.getParentId());
            ps.setString(3, spec.getName());
            ps.setInt(4, spec.getUser().getId());
            ps.setLong(5, time);
            ps.setBoolean(6, spec.isRecursive());
            ps.setInt(7, spec.getUserDeleted().getId());
            ps.setLong(8, time);
            ps.setString(9, Json.serializeToString(spec.getSearch(), null));
            ps.setString(10, Json.serializeToString(spec.getAttrs(), "{}"));
            return ps;
        });

        return getAfterCreate(spec.getFolderId());
    }

    private static final String[] UPDATE = {
            JdbcUtils.update("folder", "pk_folder",
                    "time_modified",
                    "user_modified",
                    "pk_parent",
                    "str_name",
                    "bool_recursive",
                    "json_search",
                    "json_attrs"),

            JdbcUtils.update("folder", "pk_folder",
                    "time_modified",
                    "user_modified",
                    "pk_parent",
                    "str_name",
                    "bool_recursive",
                    "json_attrs")
    };

    @Override
    public boolean update(int id, Folder folder) {
        Preconditions.checkNotNull(folder.getParentId(), "Parent folder cannot be null");
        Preconditions.checkArgument(folder.getId() >0 , "Cannot modify root folder");

        /**
         * Skip updating the search if its a dyhi so the exists statement
         * doesn't get automatically updated.  Its also
         */
        if (isDyHi(id)) {
            return jdbc.update(UPDATE[1],
                    System.currentTimeMillis(),
                    SecurityUtils.getUser().getId(),
                    folder.getParentId(),
                    folder.getName(),
                    folder.isRecursive(),
                    Json.serializeToString(folder.getAttrs(), "{}"),
                    folder.getId()) == 1;
        }
        else {
            return jdbc.update(UPDATE[0],
                    System.currentTimeMillis(),
                    SecurityUtils.getUser().getId(),
                    folder.getParentId(),
                    folder.getName(),
                    folder.isRecursive(),
                    Json.serializeToString(folder.getSearch(), null),
                    Json.serializeToString(folder.getAttrs(), "{}"),
                    folder.getId()) == 1;
        }
    }

    @Override
    public int deleteAll(DyHierarchy dyhi) {
        return jdbc.update("DELETE FROM folder WHERE pk_dyhi=?", dyhi.getId());
    }

    @Override
    public boolean delete(Folder folder) {
        if (jdbc.update("DELETE FROM folder WHERE pk_folder=?", folder.getId()) == 1) {
            return true;
        }
        return false;
    }

    @Override
    public int deleteAll(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        /**
         * The list has to be sorted from lowest to highest.  A parent folder will
         * always have a lower ID than child folders.  Hopefully this is enough
         * for a clean delete.
         */
        List<Integer> sorted = Lists.newArrayList(ids);
        Collections.sort(sorted);
        return jdbc.update("DELETE FROM folder WHERE " + JdbcUtils.in("pk_folder", ids.size()),
                sorted.toArray());
    }

    @Override
    public boolean hasAccess(Folder folder, Access access) {
        return jdbc.queryForObject(appendAccess("SELECT COUNT(1) FROM folder WHERE pk_folder=?", access),
                Integer.class, appendAclArgs(folder.getId())) > 0;
    }

    @Override
    public boolean setTaxonomyRoot(Folder folder, boolean value) {
        return jdbc.update("UPDATE folder SET bool_tax_root=? WHERE pk_folder=? AND bool_tax_root=? AND pk_folder!=0",
                value, folder.getId(), !value) == 1;
    }

    @Override
    public boolean setDyHierarchyRoot(Folder folder,  String field) {
        return jdbc.update("UPDATE folder SET bool_recursive=0, bool_dyhi_root=1, str_dyhi_field=? WHERE pk_folder=? AND pk_folder!=0", field, folder.getId()) == 1;
    }

    @Override
    public boolean removeDyHierarchyRoot(Folder folder) {
        return jdbc.update("UPDATE folder SET bool_recursive=1, bool_dyhi_root=0,str_dyhi_field=null WHERE pk_folder=? AND pk_folder!=0", folder.getId()) == 1;
    }

    @Override
    public void setAcl(int folder, Acl acl) {
        setAcl(folder, acl, true);
    }

    @Override
    public void updateAcl(int folder, Acl acl) {
        setAcl(folder, acl, false);
    }

    @Override
    public void setAcl(int folder, Acl acl, boolean replace) {

        if (acl == null || acl.isEmpty()) {
            return;
        }

        if (replace) {
            jdbc.update("DELETE FROM folder_acl WHERE pk_folder=?", folder);
            for (AclEntry entry : acl) {
                if (entry.getAccess() == 0) {
                    continue;
                }
                if (entry.getAccess() > 7) {
                    throw new ArchivistWriteException("Invalid Access level "
                            + entry.getAccess() + " for permission ID " + entry.getPermissionId());
                }
                jdbc.update("INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (?,?,?)",
                        entry.getPermissionId(), folder, entry.getAccess());
            }
        }
        else {

            for (AclEntry entry : acl) {
                if (entry.getAccess() > 7) {
                    throw new ArchivistWriteException("Invalid Access level "
                            + entry.getAccess() + " for permission ID " + entry.getPermissionId());
                }
                if (entry.getAccess() == 0) {
                    jdbc.update("DELETE FROM folder_acl WHERE pk_folder=? AND pk_permission=?",
                            folder, entry.getPermissionId());
                }
                else if (jdbc.update("UPDATE folder_acl SET int_access=? WHERE pk_folder=? AND pk_permission=?",
                        entry.getAccess(), folder, entry.getPermissionId()) != 1) {
                    jdbc.update("INSERT INTO folder_acl (pk_permission, pk_folder, int_access) VALUES (?,?,?)",
                            entry.getPermissionId(), folder, entry.getAccess());
                }
            }
        }
    }

    @Override
    public Acl getAcl(int folder) {
        Acl result = new Acl();
        jdbc.query("SELECT p.str_authority, p.pk_permission, f.int_access FROM folder_acl f,permission p WHERE f.pk_permission = p.pk_permission and f.pk_folder=?", rs -> {
            result.add(new AclEntry(rs.getString("str_authority"), rs.getInt("pk_permission"), rs.getInt("int_access")));
        }, folder);
        return result;
    }

    /**
     * Append the permissions check to the given query.
     *
     * @param query
     * @return
     */
    private String appendAccess(String query, Access access) {
        if (SecurityUtils.hasPermission("group::administrator")) {
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
        if (SecurityUtils.hasPermission("group::administrator"))  {
            return args;
        }

        List<Object> result = Lists.newArrayListWithCapacity(args.length + SecurityUtils.getPermissionIds().size());
        for (Object a: args) {
            result.add(a);
        }
        result.addAll(SecurityUtils.getPermissionIds());
        return result.toArray();
    }

    /**
     * Called by create so we can return a folder object even if we are stupid and create
     * a folder we don't have access to.
     *
     * @param id
     * @return
     */
    private Folder getAfterCreate(int id) {
        return jdbc.queryForObject(GET.concat(" WHERE pk_folder=?"), MAPPER, id);
    }

    private boolean isDyHi(int id) {
        SqlRowSet row = jdbc.queryForRowSet("SELECT pk_dyhi, bool_dyhi_root FROM folder WHERE pk_folder=?", id);
        try {
            row.next();

            if (row.getBoolean("bool_dyhi_root")) {
                return true;
            }
            if (row.getString("pk_dyhi") != null) {
                return true;
            }
        } catch (Exception e) {
            //ignore
        }
        return false;

    }
}
