package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.DyHierarchy;
import com.zorroa.archivist.domain.DyHierarchyLevel;
import com.zorroa.archivist.domain.DyHierarchySpec;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Set;

/**
 * Created by chambers on 7/14/16.
 */
@Repository
public class DyHeirarchyDaoImpl extends AbstractDao implements DyHierarchyDao {

    /**
     * The current list of working dyhi processes.
     */
    private final Set<DyHierarchy> WORKING = Sets.newConcurrentHashSet();

    private static final String INSERT =
            JdbcUtils.insert("dyhi",
                    "pk_folder",
                    "int_user_created",
                    "time_created",
                    "int_levels",
                    "json_levels");

    @Override
    public DyHierarchy create(DyHierarchySpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_dyhi"});
            ps.setInt(1, spec.getFolderId());
            ps.setInt(2,  SecurityUtils.getUser().getId());
            ps.setLong(3, System.currentTimeMillis());
            ps.setInt(4, spec.getLevels().size());
            ps.setString(5, Json.serializeToString(spec.getLevels(), "[]"));
            return ps;
        }, keyHolder);
        return get(keyHolder.getKey().intValue());
    }

    private final RowMapper<DyHierarchy> MAPPER = (rs, row) -> {
        DyHierarchy h = new DyHierarchy();
        h.setFolderId(rs.getInt("pk_folder"));
        h.setId(rs.getInt("pk_dyhi"));
        h.setTimeCreated(rs.getLong("time_created"));
        h.setUserCreated(resolveUser(rs.getInt("int_user_created")));
        h.setLevels(Json.deserialize(rs.getString("json_levels"),
                new TypeReference<List<DyHierarchyLevel>>() {}));
        h.setWorking(WORKING.contains(h));

        return h;
    };

    private static final String GET =
            "SELECT " +
                "pk_dyhi,"+
                "pk_folder, " +
                "int_user_created, " +
                "time_created,"+
                "int_levels," +
                "json_levels " +
            "FROM "+
                "dyhi ";
    @Override
    public DyHierarchy get(int id) {
        return jdbc.queryForObject(GET.concat(" WHERE pk_dyhi=?"), MAPPER, id);
    }

    @Override
    public DyHierarchy refresh(DyHierarchy object) {
        return get(object.getId());
    }

    @Override
    public List<DyHierarchy> getAll() {
        return jdbc.query(GET, MAPPER);
    }

    @Override
    public PagedList<DyHierarchy> getAll(Paging page) {
        return new PagedList(page.setTotalCount(count()),
                jdbc.query(GET.concat("ORDER BY pk_dyhi LIMIT ? OFFSET ?"),
                        MAPPER, page.getSize(), page.getFrom()));
    }

    private static String UPDATE = JdbcUtils.update("dyhi", "pk_dyhi",
            "pk_folder",
            "int_levels",
            "json_levels");

    @Override
    public boolean update(int id, DyHierarchySpec spec) {
        return jdbc.update(UPDATE, spec.getFolderId(), spec.getLevels().size(),
                Json.serializeToString(spec.getLevels(), "[]")) == 1;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM dyhi WHERE pk_dyhi=?", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM dyhi", Integer.class);
    }

    @Override
    public boolean isWorking(DyHierarchy d) {
        return WORKING.contains(d);
    }

    @Override
    public boolean setWorking(DyHierarchy d, boolean value) {
        if (value) {
            return WORKING.add(d);
        }
        else {
            return WORKING.remove(d);
        }
    }
}
