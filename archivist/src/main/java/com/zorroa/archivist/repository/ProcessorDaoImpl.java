package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Plugin;
import com.zorroa.archivist.domain.Processor;
import com.zorroa.archivist.domain.ProcessorFilter;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.processor.ProcessorSpec;
import com.zorroa.sdk.processor.ProcessorType;
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
                    "int_type",
                    "str_description",
                    "json_display",
                    "json_filters",
                    "json_file_types",
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
            ps.setInt(5, spec.getType().ordinal());
            ps.setString(6, spec.getDescription() == null ? shortName : spec.getDescription());
            ps.setString(7, Json.serializeToString(spec.getDisplay(), "[]"));
            ps.setString(8, Json.serializeToString(spec.getFilters(), "[]"));
            ps.setString(9,Json.serializeToString(spec.getFileTypes(), "[]"));
            ps.setLong(10, time);
            ps.setLong(11, time);
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
        p.setType(ProcessorType.values()[rs.getInt("int_type")]);
        p.setDescription(rs.getString("str_description"));
        p.setDisplay(Json.deserialize(rs.getString("json_display"), new TypeReference<List<Map<String, Object>>>() {}));
        p.setFilters(Json.deserialize(rs.getString("json_filters"), Json.LIST_OF_STRINGS));
        p.setFileTypes(Json.deserialize(rs.getString("json_file_types"), Json.SET_OF_STRINGS));
        p.setPluginId(rs.getInt("pk_plugin"));
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
            "processor.int_type,"+
            "processor.str_description,"+
            "processor.json_display,"+
            "processor.json_filters, " +
            "processor.json_file_types,"+
            "plugin.pk_plugin, "+
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
        return jdbc.query(GET.concat(" ORDER BY processor.str_short_name"), MAPPER);
    }

    @Override
    public List<Processor> getAll(ProcessorFilter filter) {

        if (!JdbcUtils.isValid(filter.getSort())) {
            filter.setSort(ImmutableMap.of("shortName", "asc"));
        }
        String q = filter.getQuery(GET, null);
        return jdbc.query(q, MAPPER, filter.getValues());
    }

    @Override
    public List<Processor> getAll(Plugin plugin) {
        return jdbc.query(GET.concat(" WHERE plugin.pk_plugin=?"), MAPPER, plugin.getId());
    }

    @Override
    public PagedList<Processor> getAll(Pager page) {
        return new PagedList<>(
                page.setTotalCount(count()),
                jdbc.query(GET.concat("ORDER BY processor.str_short_name LIMIT ? OFFSET ?"), MAPPER,
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
    public boolean deleteAll(Plugin plugin) {
        return jdbc.update("DELETE FROM processor WHERE processor.pk_plugin=?", plugin.getId()) > 0;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM processor", Long.class);
    }

    private final RowMapper<ProcessorRef> REF_MAPPER = (rs, row) -> {
        ProcessorRef ref = new ProcessorRef();
        ref.setType(ProcessorType.values()[rs.getInt("int_type")]);
        ref.setClassName(rs.getString("str_name"));
        ref.setLanguage(rs.getString("plugin_lang"));
        ref.setFileTypes(Json.deserialize(rs.getString("json_file_types"), Json.SET_OF_STRINGS));
        ref.setFilters(Lists.newArrayList());
        List<String> filters = Json.deserialize(rs.getString("json_filters"), Json.LIST_OF_STRINGS);
        for (String filt: filters) {
            ref.getFilters().add(new com.zorroa.sdk.processor.ProcessorFilter(filt));
        }
        return ref;
    };

    private static final String GET_REF =
        "SELECT " +
            "processor.str_name,"+
            "processor.int_type,"+
            "processor.json_filters, "+
            "processor.json_file_types,"+
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
