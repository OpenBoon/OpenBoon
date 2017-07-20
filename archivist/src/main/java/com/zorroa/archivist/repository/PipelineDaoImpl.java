package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpecV;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.processor.ProcessorRef;
import com.zorroa.sdk.util.Json;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class PipelineDaoImpl extends AbstractDao implements PipelineDao {

    private static final RowMapper<Pipeline> MAPPER = (rs, row) -> {
        Pipeline result = new Pipeline();
        result.setId(rs.getInt("pk_pipeline"));
        result.setName(rs.getString("str_name"));
        result.setProcessors(Json.deserialize(rs.getString("json_processors"), ProcessorRef.LIST_OF));
        result.setType(PipelineType.values()[rs.getInt("int_type")]);
        result.setDescription(rs.getString("str_description"));
        result.setVersion(rs.getInt("int_version"));
        return result;
    };

    private static final String INSERT =
            JdbcUtils.insert("pipeline",
                    "str_name",
                    "int_type",
                    "json_processors",
                    "str_description",
                    "bool_standard");

    @Override
    public Pipeline create(PipelineSpecV spec) {
        Preconditions.checkNotNull(spec.getName());
        Preconditions.checkNotNull(spec.getType());
        Preconditions.checkNotNull(spec.getProcessors());

        if (spec.getDescription() == null) {
            spec.setDescription(spec.getName() + " " +
                    spec.getType() + " pipeline created by " + SecurityUtils.getUsername());
        }


        if (spec.isStandard()) {
            jdbc.update("UPDATE pipeline SET bool_standard=0 WHERE bool_standard=1");
        }

        /*
         * If there are no pipelines, then this one is the standard.
         */
        if (count() == 0) {
            spec.setStandard(true);
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setString(1, spec.getName());
            ps.setInt(2, spec.getType().ordinal());
            ps.setString(3, Json.serializeToString(spec.getProcessors(), "[]"));
            ps.setString(4, spec.getDescription());
            ps.setBoolean(5, spec.isStandard());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public Pipeline getStandard() {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE bool_standard=1 LIMIT 1", MAPPER);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to find a standard pipeline", 1);
        }
    }

    @Override
    public Pipeline get(int id) {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE pk_pipeline=?", MAPPER, id);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to get pipeline: id=" + id, 1);
        }
    }

    @Override
    public Pipeline get(String name) {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE str_name=?", MAPPER, name);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to get pipeline: id=" + name, 1);
        }
    }

    @Override
    public Pipeline refresh(Pipeline p) {
        return get(p.getId());
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline WHERE str_name=?", Integer.class, name) == 1;
    }

    @Override
    public List<Pipeline> getAll() {
        return jdbc.query("SELECT * FROM pipeline", MAPPER);
    }

    @Override
    public PagedList<Pipeline> getAll(Pager page) {
        return new PagedList<>(
                page.setTotalCount(count()),
                    jdbc.query("SELECT * FROM pipeline ORDER BY pk_pipeline LIMIT ? OFFSET ?", MAPPER,
                    page.getSize(), page.getFrom()));
    }

    private static final String UPDATE =
            JdbcUtils.update("pipeline", "pk_pipeline",
                    "str_name",
                    "int_type",
                    "json_processors",
                    "str_description",
                    "bool_standard",
                    "int_version=int_version+?");

    @Override
    public boolean update(int id, Pipeline spec) {
        // Unset standard if its set.
        if (spec.isStandard()) {
            jdbc.update("UPDATE pipeline SET bool_standard=0 WHERE bool_standard=1");
        }
        int incrementVersion = 0;
        if (spec.getVersionUp() != null && spec.getVersionUp()) {
            incrementVersion=1;
        }

        return jdbc.update(UPDATE, spec.getName(), spec.getType().ordinal(),
                Json.serializeToString(spec.getProcessors()), spec.getDescription(),
                spec.isStandard(), incrementVersion, id) == 1;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=? AND bool_standard=0", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline", Long.class);
    }
}
