package com.zorroa.archivist.repository;

import com.google.common.collect.Sets;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Acl;
import com.zorroa.archivist.domain.Folder;
import com.zorroa.archivist.domain.TrashedFolder;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 12/2/16.
 */
@Repository
public class TrashFolderDaoImpl extends AbstractDao implements TrashFolderDao {

    @Autowired
    UserDaoCache userDaoCache;

    private static final String INSERT =
            JdbcUtils.insert("folder_trash",
                    "pk_folder",
                    "pk_parent",
                    "str_opid",
                    "str_name",
                    "json_search",
                    "json_acl",
                    "bool_recursive",
                    "user_created",
                    "time_created",
                    "time_modified",
                    "user_deleted",
                    "time_deleted",
                    "bool_primary",
                    "int_order",
                    "json_attrs");

    @Override
    public int create(Folder folder, String opid, boolean primary, int order) {
        long time = System.currentTimeMillis();
        int user = SecurityUtils.getUser().getId();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_folder_trash"});
            ps.setInt(1, folder.getId());
            ps.setInt(2, folder.getParentId());
            ps.setString(3, opid);
            ps.setString(4, folder.getName());
            ps.setString(5, Json.serializeToString(folder.getSearch(), null));
            ps.setString(6, Json.serializeToString(folder.getAcl(), null));
            ps.setBoolean(7, folder.isRecursive());
            ps.setInt(8, folder.getUser().getId());
            ps.setLong(9, folder.getTimeCreated());
            ps.setLong(10, folder.getTimeModified());
            ps.setInt(11, user);
            ps.setLong(12, time);
            ps.setBoolean(13, primary);
            ps.setInt(14, order);
            ps.setString(15, Json.serializeToString(folder.getAttrs(), "{}"));
            return ps;
        }, keyHolder);

        return keyHolder.getKey().intValue();
    }

    private final RowMapper<TrashedFolder> MAPPER = (rs, row) -> {
        TrashedFolder folder = new TrashedFolder();
        folder.setOpId(rs.getString("str_opid"));
        folder.setId(rs.getInt("pk_folder_trash"));
        folder.setFolderId(rs.getInt("pk_folder"));
        folder.setName(rs.getString("str_name"));
        folder.setUser(userDaoCache.getUser(rs.getInt("user_created")));
        folder.setUserDeleted(userDaoCache.getUser(rs.getInt("user_deleted")));
        folder.setRecursive(rs.getBoolean("bool_recursive"));
        folder.setTimeCreated(rs.getLong("time_created"));
        folder.setTimeModified(rs.getLong("time_modified"));
        folder.setTimeModified(rs.getLong("time_deleted"));

        Object parent = rs.getObject("pk_parent");
        if (parent != null) {
            folder.setParentId((Integer) parent);
        }

        String searchString = rs.getString("json_search");
        if (searchString != null) {
            folder.setSearch(Json.deserialize(searchString, AssetSearch.class));
        }

        String aclString = rs.getString("json_acl");
        if (aclString != null) {
            folder.setAcl( Json.deserialize(aclString, Acl.class));
        }

        String attrs = rs.getString("json_attrs");
        folder.setAttrs(Json.deserialize(attrs, Map.class));

        return folder;
    };

    private static final String GET = "SELECT * FROM folder_trash ";

    @Override
    public TrashedFolder get(int id, int user) {
        return jdbc.queryForObject(
                GET.concat(" WHERE pk_folder_trash=? AND user_deleted=?"), MAPPER, id, user);
    }

    @Override
    public List<TrashedFolder> getAll(int user) {
        return jdbc.query(
                GET.concat(" WHERE user_deleted=? AND bool_primary=1"), MAPPER, user);
    }

    @Override
    public int count(int user) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM folder_trash WHERE user_deleted=? AND bool_primary=1",
                Integer.class, user);
    }

    @Override
    public List<TrashedFolder> getAll(Folder parent, int user) {
        return jdbc.query(
                GET.concat(" WHERE pk_parent=? AND user_deleted=? AND bool_primary=1"), MAPPER,
                parent.getId(), user);
    }

    @Override
    public List<TrashedFolder> getAll(String opId) {
        return jdbc.query(GET.concat(" WHERE str_opid=? ORDER BY int_order DESC"), MAPPER, opId);
    }

    @Override
    public List<Integer> getAllIds(String opId) {
        return jdbc.queryForList(
                "SELECT pk_folder_trash FROM folder_trash WHERE str_opid=? ORDER BY int_order DESC", Integer.class, opId);
    }

    @Override
    public List<Integer> getAllIds(int user) {
        return jdbc.queryForList(
                "SELECT pk_folder_trash FROM folder_trash WHERE user_deleted=? ORDER BY int_order DESC", Integer.class, user);
    }

    @Override
    public List<Integer> removeAll(String opId) {
        List<Integer> ids = getAllIds(opId);
        if (jdbc.update("DELETE FROM folder_trash WHERE str_opid=?", opId) == ids.size()) {
            return ids;
        }
        else {
            List<Integer> leftOver = getAllIds(opId);
            leftOver.removeAll(ids);
            return leftOver;
        }
    }

    @Override
    public List<Integer> removeAll(List<Integer> ids, int user) {
        Set<String> opIds = Sets.newHashSet();
        for (int id: ids) {
            try {
                TrashedFolder folder = get(id, user);
                if (!opIds.contains(folder.getOpId())) {
                    opIds.add(folder.getOpId());
                    removeAll(folder.getOpId());
                }
            } catch (EmptyResultDataAccessException e) {
                logger.warn("Unable to find trash folder id: {}", id);
            }
        }
        return ids;
    }

    @Override
    public List<Integer> removeAll(int user) {
        List<Integer> ids = getAllIds(user);
        if (jdbc.update("DELETE FROM folder_trash WHERE user_deleted=?", user) == ids.size()) {
            return ids;
        }
        else {
            List<Integer> leftOver = getAllIds(user);
            leftOver.removeAll(ids);
            return leftOver;
        }
    }
}
