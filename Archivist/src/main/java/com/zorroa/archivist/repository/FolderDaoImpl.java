package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.sdk.domain.AssetSearch;
import com.zorroa.archivist.sdk.domain.Folder;
import com.zorroa.archivist.sdk.domain.FolderBuilder;
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

    private static final RowMapper<Folder> MAPPER = (rs, row) -> {
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

        return folder;
    };

    private static final String GET =
            "SELECT " +
                "* " +
            "FROM " +
                "folder ";
    @Override
    public Folder get(int id) {
        return jdbc.queryForObject(GET + " WHERE pk_folder=?", MAPPER, id);
    }

    @Override
    public Folder get(int parent, String name) {
        return jdbc.queryForObject(GET + " WHERE pk_parent=? and str_name=?", MAPPER, parent, name);
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
        sb.append(" WHERE pk_folder=?");

        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(Folder folder) {
        return jdbc.update("DELETE FROM folder WHERE pk_folder=?", folder.getId()) ==1;
    }
}
