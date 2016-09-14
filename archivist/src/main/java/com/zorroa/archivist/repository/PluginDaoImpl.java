package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.PluginSpec;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 8/16/16.
 */
@Repository
public class PluginDaoImpl extends AbstractDao implements PluginDao {

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM plugin WHERE str_name=?", Integer.class, name) > 0;
    }

    private static final String INSERT =
            JdbcUtils.insert("plugin",
                    "str_name",
                    "str_version",
                    "str_lang",
                    "str_description",
                    "str_publisher",
                    "time_created",
                    "time_modified",
                    "str_md5");

    @Override
    public Plugin create(PluginSpec spec) {
        Preconditions.checkNotNull(spec.getName());
        Preconditions.checkNotNull(spec.getDescription());
        Preconditions.checkNotNull(spec.getVersion());
        Preconditions.checkNotNull(spec.getPublisher());
        Preconditions.checkNotNull(spec.getMd5());

        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_plugin"});
            ps.setString(1, spec.getName());
            ps.setString(2, spec.getVersion());
            ps.setString(3, spec.getLanguage());
            ps.setString(4, spec.getDescription());
            ps.setString(5, spec.getPublisher());
            ps.setLong(6, time);
            ps.setLong(7, time);
            ps.setString(8, spec.getMd5());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private static final RowMapper<Plugin> MAPPER = (rs, row) -> {
        Plugin result = new Plugin();
        result.setId(rs.getInt("pk_plugin"));
        result.setName(rs.getString("str_name"));
        result.setDescription(rs.getString("str_description"));
        result.setLanguage(rs.getString("str_lang"));
        result.setVersion(rs.getString("str_version"));
        result.setPublisher(rs.getString("str_publisher"));
        result.setMd5(rs.getString("str_md5"));
        return result;
    };

    private static final String GET =
            "SELECT " +
                "pk_plugin,"+
                "str_name,"+
                "str_version,"+
                "str_description,"+
                "str_publisher,"+
                "str_lang, " +
                "str_md5 " +
            "FROM " +
                "plugin ";

    @Override
    public Plugin get(int id) {
        return jdbc.queryForObject(GET.concat(" WHERE pk_plugin=?"), MAPPER, id);
    }

    @Override
    public Plugin get(String name) {
        return jdbc.queryForObject(GET.concat(" WHERE str_name=?"), MAPPER, name);
    }

    @Override
    public Plugin refresh(Plugin object) {
        return get(object.getId());
    }

    @Override
    public List<Plugin> getAll() {
        return jdbc.query(GET, MAPPER);
    }

    @Override
    public PagedList<Plugin> getAll(Paging page) {
        return new PagedList<>(
                page.setTotalCount(count()),
                jdbc.query(GET.concat("ORDER BY str_name LIMIT ? OFFSET ?"), MAPPER,
                        page.getSize(), page.getFrom()));
    }

    @Override
    public boolean update(int id, Plugin spec) {
        throw new NotImplementedException("Not implemented, try 'boolean update(int id, PluginSpec spec)'");
    }

    private static final String UPDATE =
        JdbcUtils.update("plugin", "pk_plugin", "str_version", "time_modified", "str_md5");

    @Override
    public boolean update(int id, PluginSpec spec) {
        return jdbc.update(UPDATE, spec.getVersion(), System.currentTimeMillis(), spec.getMd5(), id) == 1;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM plugin WHERE pk_plugin=?", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM plugin", Long.class);
    }
}
