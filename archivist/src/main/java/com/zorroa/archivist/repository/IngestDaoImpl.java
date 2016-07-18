package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Ingest;
import com.zorroa.archivist.domain.IngestSpec;
import com.zorroa.archivist.domain.Schedule;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.plugins.ModuleRef;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 7/9/16.
 */
@Repository
public class IngestDaoImpl extends AbstractDao implements IngestDao {

    private final RowMapper<Ingest> MAPPER = (rs, row) -> {
        Ingest i = new Ingest();
        i.setId(rs.getInt("pk_ingest"));
        i.setName(rs.getString("str_name"));
        i.setAutomatic(rs.getBoolean("bool_automatic"));
        i.setSchedule(new Schedule(rs.getString("crond_trigger")));
        i.setGenerators(Json.deserialize(rs.getString("json_generators"), ModuleRef.LIST_TYPE_REF));
        i.setTimeCreated(rs.getLong("time_created"));
        i.setTimeExecuted(rs.getLong("time_executed"));
        i.setUserCreated(rs.getInt("int_user_created"));

        Object folder = rs.getObject("pk_folder");
        if (folder != null) {
            i.setFolderId((Integer) folder);
        }

        Object pipeline = rs.getString("json_pipeline");
        if (pipeline != null) {
            i.setPipeline(Json.deserialize(pipeline.toString(), ModuleRef.LIST_TYPE_REF));
        }

        Object pipelineId = rs.getObject("pk_pipeline");
        if (pipelineId != null) {
            i.setPipelineId((Integer) pipelineId);
        }

        return i;
    };

    private static final String INSERT =
            JdbcUtils.insert("ingest",
                "pk_pipeline",
                "pk_folder",
                "str_name",
                "time_created",
                "int_user_created",
                "crond_trigger",
                "bool_automatic",
                "json_generators",
                "json_pipeline");

    @Override
    public Ingest create(IngestSpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_ingest"});
            ps.setObject(1, spec.getPipelineId());
            ps.setObject(2, spec.getFolderId());
            ps.setString(3, spec.getName());
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, SecurityUtils.getUser().getId());
            ps.setString(6, spec.getSchedule().toString());
            ps.setBoolean(7, spec.isAutomatic());
            ps.setString(8, Json.serializeToString(spec.getGenerators(), "[]"));

            if (spec.getPipeline() != null) {
                ps.setString(9, Json.serializeToString(spec.getPipeline()));
            }
            else {
                ps.setString(9, null);
            }
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public Ingest get(String name) {
        return jdbc.queryForObject("SELECT * FROM ingest WHERE str_name=?", MAPPER, name);
    }

    @Override
    public Ingest get(int id) {
        return jdbc.queryForObject("SELECT * FROM ingest WHERE pk_ingest=?", MAPPER, id);
    }

    @Override
    public Ingest refresh(Ingest object) {
        return get(object.getId());
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM ingest WHERE str_name=?",
                Integer.class, name) > 0;
    }

    @Override
    public List<Ingest> getAll() {
        return jdbc.query("SELECT * FROM ingest", MAPPER);
    }

    @Override
    public PagedList<Ingest> getAll(Paging page) {
        return new PagedList(page.setTotalCount(count()),
                    jdbc.query("SELECT * FROM ingest ORDER BY pk_ingest LIMIT ? OFFSET ?",
                        MAPPER, page.getSize(), page.getFrom()));
    }

    private static final String UPDATE =
            JdbcUtils.update("ingest", "pk_ingest",
                    "pk_pipeline",
                    "pk_folder",
                    "str_name",
                    "crond_trigger",
                    "bool_automatic",
                    "json_generators",
                    "json_pipeline");

    @Override
    public boolean update(int id, IngestSpec spec) {
        return jdbc.update(UPDATE,
                spec.getPipelineId(),
                spec.getFolderId(),
                spec.getName(),
                spec.getSchedule().toString(),
                spec.isAutomatic(),
                Json.serializeToString(spec.getGenerators(), "[]"),
                spec.getPipeline() == null ? null : Json.serializeToString(spec.getPipeline()),
                id) > 0;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM ingest WHERE pk_ingest = ?", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM ingest", Long.class);
    }
}
