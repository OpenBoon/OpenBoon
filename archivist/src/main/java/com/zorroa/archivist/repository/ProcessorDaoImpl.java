package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.ProcessorSpec;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.util.Json;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * Note that, the "name" of a processor is its class name.
 */
@Repository
public class ProcessorDaoImpl extends AbstractDao implements ProcessorDao {

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM processor WHERE str_name=?", Integer.class, name) == 1;
    }

    private static final String INSERT =
            JdbcUtils.insert("processor",
                    "pk_plugin",
                    "str_name",
                    "str_short_name",
                    "str_module",
                    "str_type",
                    "str_description",
                    "json_display",
                    "json_ext",
                    "time_created",
                    "time_modified");

    @Override
    public Processor create(Plugin plugin, ProcessorSpec spec) {
        Preconditions.checkNotNull(spec.getType());
        Preconditions.checkNotNull(spec.getClassName());
        Preconditions.checkNotNull(spec.getDisplay());

        if (!spec.getClassName().contains(".")) {
            throw new IllegalArgumentException("Processor class name has no module, must be named 'something.Name'");
        }

        String shortName = spec.getClassName().substring(spec.getClassName().lastIndexOf('.')+1);
        String module =  spec.getClassName().substring(0, spec.getClassName().lastIndexOf('.'));

        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_processor"});
            ps.setInt(1, plugin.getId());
            ps.setString(2, spec.getClassName());
            ps.setString(3, shortName);
            ps.setString(4, module);
            ps.setString(5, spec.getType());
            ps.setString(6, spec.getDescription() == null ? shortName : spec.getDescription());
            ps.setString(7, Json.serializeToString(spec.getDisplay(), "[]"));
            ps.setString(8, Json.serializeToString(spec.getSupportedExtensions(), "[]"));
            ps.setLong(9, time);
            ps.setLong(10, time);
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private static final RowMapper<Processor> MAPPER = (rs, row) -> {
        Processor p = new Processor();
        p.setId(rs.getInt("pk_processor"));
        p.setName(rs.getString("str_name"));
        p.setShortName(rs.getString("str_short_name"));
        p.setModule(rs.getString("str_module"));
        p.setType(rs.getString("str_type"));
        p.setDescription(rs.getString("str_description"));
        p.setDisplay(Json.deserialize(rs.getString("json_display"), new TypeReference<List<Map<String, Object>>>() {}));
        p.setSupportedExtensions(Json.deserialize(rs.getString("json_ext"), Json.SET_OF_STRINGS));
        p.setPluginLanguage(rs.getString("plugin_lang"));
        p.setPluginVersion(rs.getString("plugin_ver"));
        p.setPluginName(rs.getString("plugin_name"));
        return p;
    };

    private static final String GET =
        "SELECT " +
            "processor.pk_processor,"+
            "processor.str_name,"+
            "processor.str_short_name,"+
            "processor.str_module,"+
            "processor.str_type,"+
            "processor.str_description,"+
            "processor.json_display,"+
            "processor.json_ext, " +
            "plugin.str_name AS plugin_name, "+
            "plugin.str_lang AS plugin_lang, " +
            "plugin.str_version AS plugin_ver " +
        "FROM " +
            "processor JOIN plugin ON ( processor.pk_plugin = plugin.pk_plugin ) ";

    @Override
    public Processor get(String name) {
        return jdbc.queryForObject(GET.concat(" WHERE processor.str_name=?"), MAPPER, name);
    }

    @Override
    public Processor get(int id) {
        return jdbc.queryForObject(GET.concat(" WHERE processor.pk_processor=?"), MAPPER, id);
    }

    @Override
    public Processor refresh(Processor object) {
        return get(object.getId());
    }

    @Override
    public List<Processor> getAll() {
        return jdbc.query(GET, MAPPER);
    }

    @Override
    public List<Processor> getAll(ProcessorFilter filter) {
        String q = filter.getQuery(GET, null);
        return jdbc.query(q, MAPPER, filter.getValues());
    }

    @Override
    public List<Processor> getAll(Plugin plugin) {
        return jdbc.query(GET.concat(" WHERE plugin.pk_plugin=?"), MAPPER, plugin.getId());
    }

    @Override
    public PagedList<Processor> getAll(Paging page) {
        return new PagedList<>(
                page.setTotalCount(count()),
                jdbc.query(GET.concat("ORDER BY processor.str_name LIMIT ? OFFSET ?"), MAPPER,
                        page.getSize(), page.getFrom()));
    }

    @Override
    public boolean update(int id, Processor spec) {
        return false;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM processor WHERE processor.pk_processor=?", id) > 0;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM processor", Long.class);
    }

    private static final RowMapper<ProcessorRef> REF_MAPPER = (rs, row) -> {
        ProcessorRef ref = new ProcessorRef();
        ref.setClassName(rs.getString("str_name"));
        ref.setLanguage(rs.getString("plugin_lang"));
        return ref;
    };

    private static final String GET_REF =
        "SELECT " +
            "processor.str_name,"+
            "plugin.str_lang AS plugin_lang " +
        "FROM " +
            "processor INNER JOIN plugin ON ( processor.pk_plugin = plugin.pk_plugin ) " +
        "WHERE " +
            "processor.str_name=?";

    @Override
    public ProcessorRef getRef(String name) {
        try {
            return jdbc.queryForObject(GET_REF, REF_MAPPER, name);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to find Processor '" + name + "'", 1, e);
        }
    }
}
