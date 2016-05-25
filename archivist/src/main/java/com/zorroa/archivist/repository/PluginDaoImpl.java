package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.sdk.plugins.PluginProperties;
import com.zorroa.sdk.processor.DisplayProperties;
import com.zorroa.sdk.processor.ProcessorProperties;
import com.zorroa.sdk.processor.ProcessorType;
import com.zorroa.sdk.util.Json;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 5/20/16.
 */
@Repository
public class PluginDaoImpl extends AbstractDao implements PluginDao {

    private static final String INSERT_PLUGIN =
            JdbcUtils.insert("plugin",
                    "str_name",
                    "str_version",
                    "str_description");

    @Override
    public int create(PluginProperties plugin) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps =
                        connection.prepareStatement(INSERT_PLUGIN, new String[]{"pk_plugin"});
                ps.setString(1, plugin.getName());
                ps.setString(2, plugin.getVersion());
                ps.setString(3, plugin.getDescription());
                return ps;
            }, keyHolder);
            return keyHolder.getKey().intValue();

        } catch (DuplicateKeyException e) {
            return jdbc.queryForObject("SELECT pk_plugin FROM plugin WHERE str_name=? AND str_version=?",
                    Integer.class, plugin.getName(), plugin.getVersion());
        }
    }

    private static final String INSERT_PROCESSOR =
            JdbcUtils.insert("processor",
                    "pk_plugin",
                    "str_name",
                    "int_type",
                    "json_display");

    private static final String UPDATE_PROCESSOR =
            "UPDATE " +
                "processor " +
            "SET " +
                "json_display=? " +
            "WHERE " +
                "pk_plugin=? " +
            "AND " +
                "str_name=? ";

    @Override
    public void addProcessor(int pluginId, ProcessorProperties processor) {
        Preconditions.checkNotNull(processor.getType(), "The processor type cannot be null");

        logger.info("adding processor: {} {} {}", pluginId, processor.getClassName(), processor.getDisplay());
        if (jdbc.update(UPDATE_PROCESSOR,
                Json.serializeToString(processor.getDisplay()),
                pluginId,
                processor.getClassName()) == 0) {
            try {
                jdbc.update(connection -> {
                    PreparedStatement ps =
                            connection.prepareStatement(INSERT_PROCESSOR);
                    ps.setInt(1, pluginId);
                    ps.setString(2, processor.getClassName());
                    ps.setInt(3, processor.getType().ordinal());
                    ps.setString(4, Json.serializeToString(processor.getDisplay()));
                    return ps;
                });
            } catch (DuplicateKeyException e) {
                logger.warn("", e);
            }
        }
    }

    private static final RowMapper<PluginProperties> MAPPER_PLUGIN = (rs, row) -> {
        PluginProperties d = new PluginProperties();
        d.setName(rs.getString("str_name"));
        d.setVersion(rs.getString("str_version"));
        d.setDescription(rs.getString("str_description"));
        return d;
    };

    @Override
    public List<PluginProperties> getPlugins() {
        return jdbc.query("SELECT * FROM plugin", MAPPER_PLUGIN);
    }

    private static final String GET_PROCS =
            "SELECT * FROM processor";

    private static final RowMapper<ProcessorProperties> MAPPER_PROC = (rs, row) -> {
        ProcessorProperties d = new ProcessorProperties();
        d.setClassName(rs.getString("str_name"));
        d.setType(ProcessorType.values()[rs.getInt("int_type")]);
        d.setDisplay(Json.deserialize(rs.getString("json_display"),
                new TypeReference<List<DisplayProperties>>() {}));
        return d;
    };

    @Override
    public List<ProcessorProperties> getProcessors(ProcessorType type) {
        return jdbc.query(GET_PROCS + " WHERE int_type=?", MAPPER_PROC, type.ordinal());
    }

    @Override
    public List<ProcessorProperties> getProcessors() {
        return jdbc.query(GET_PROCS, MAPPER_PROC);
    }

}
