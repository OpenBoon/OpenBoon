package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Preconditions;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Pipeline;
import com.zorroa.archivist.domain.PipelineSpec;
import com.zorroa.archivist.domain.PipelineType;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.processor.ProcessorSpec;
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
        result.setProcessors(Json.deserialize(rs.getString("json_processors"),
                new TypeReference<List<ProcessorSpec>>() {}));
        result.setType(PipelineType.values()[rs.getInt("int_type")]);
        return result;
    };

    private static final String INSERT =
            JdbcUtils.insert("pipeline",
                    "str_name",
                    "int_type",
                    "json_processors");

    @Override
    public Pipeline create(PipelineSpec spec) {
        Preconditions.checkNotNull(spec.getName());
        Preconditions.checkNotNull(spec.getType());
        Preconditions.checkNotNull(spec.getProcessors());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setString(1, spec.getName());
            ps.setInt(2, spec.getType().ordinal());
            ps.setString(3, Json.serializeToString(spec.getProcessors(), "[]"));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
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
    public PagedList<Pipeline> getAll(Paging page) {
        return new PagedList<>(
                page.setTotalCount(count()),
                    jdbc.query("SELECT * FROM pipeline ORDER BY pk_pipeline LIMIT ? OFFSET ?", MAPPER,
                    page.getSize(), page.getFrom()));
    }

    private static final String UPDATE =
            JdbcUtils.update("pipeline", "pk_pipeline",
                    "str_name",
                    "int_type",
                    "json_processors");

    @Override
    public boolean update(int id, PipelineSpec spec) {
        return jdbc.update(UPDATE, spec.getName(), spec.getType().ordinal(),
                Json.serializeToString(spec.getProcessors()), id) == 1;
    }

    @Override
    public boolean delete(int id) {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=?", id) == 1;
    }

    @Override
    public long count() {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline", Long.class);
    }
}
