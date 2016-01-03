package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.util.Json;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.archivist.sdk.domain.IngestPipeline;
import com.zorroa.archivist.sdk.domain.IngestPipelineBuilder;
import com.zorroa.archivist.sdk.domain.IngestPipelineUpdateBuilder;
import com.zorroa.archivist.sdk.processor.ProcessorFactory;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class IngestPipelineDaoImpl extends AbstractDao implements IngestPipelineDao {

    private static final RowMapper<IngestPipeline> MAPPER = (rs, row) -> {
        IngestPipeline result = new IngestPipeline();
        result.setId(rs.getInt("pk_pipeline"));
        result.setName(rs.getString("str_name"));
        result.setDescription(rs.getString("str_description"));
        result.setTimeCreated(rs.getLong("time_created"));
        result.setUserCreated(rs.getInt("user_created"));
        result.setTimeModified(rs.getLong("time_modified"));
        result.setUserModified(rs.getInt("user_modified"));
        result.setProcessors(Json.deserialize(rs.getString("json_processors"),
                new TypeReference<List<ProcessorFactory<IngestProcessor>>>() {}));
        return result;
    };

    private static final String INSERT =
            "INSERT INTO " +
                    "pipeline " +
            "(" +
                    "str_name,"+
                    "str_description,"+
                    "user_created,"+
                    "time_created,"+
                    "user_modified, "+
                    "time_modified, "+
                    "json_processors " +
            ") "+
            "VALUES (?,?,?,?,?,?,?)";

    @Override
    public IngestPipeline create(IngestPipelineBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_pipeline"});
            ps.setString(1, builder.getName());
            ps.setString(2, builder.getDescription());
            ps.setInt(3, SecurityUtils.getUser().getId());
            ps.setLong(4, time);
            ps.setInt(5, SecurityUtils.getUser().getId());
            ps.setLong(6, time);
            ps.setObject(7, Json.serializeToString(builder.getProcessors()));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public IngestPipeline get(int id) {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE pk_pipeline=?", MAPPER, id);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to get pipeline: id=" + id, 1);
        }
    }

    @Override
    public IngestPipeline get(String name) {
        try {
            return jdbc.queryForObject("SELECT * FROM pipeline WHERE str_name=?", MAPPER, name);
        } catch(EmptyResultDataAccessException e) {
            throw new EmptyResultDataAccessException("Failed to get pipeline: id=" + name, 1);
        }
    }

    @Override
    public boolean exists(String name) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline WHERE str_name=?", Integer.class, name) == 1;
    }

    @Override
    public List<IngestPipeline> getAll() {
        return jdbc.query("SELECT * FROM pipeline", MAPPER);
    }

    @Override
    public boolean update(IngestPipeline pipeline, IngestPipelineUpdateBuilder builder) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE pipeline SET ");

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (builder.getDescription() != null) {
            updates.add("str_description=?");
            values.add(builder.getDescription());
        }

        if (builder.getName() != null) {
            updates.add("str_name=?");
            values.add(builder.getName());
        }

        if (builder.getProcessors() != null) {
            updates.add("json_processors=?");
            values.add(Json.serializeToString(builder.getProcessors()));
        }

        if (updates.isEmpty()) {
            return false;
        }

        updates.add("user_modified=?");
        values.add(SecurityUtils.getUser().getId());

        updates.add("time_modified=?");
        values.add(System.currentTimeMillis());

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_pipeline=?");
        values.add(pipeline.getId());

        logger.debug("{} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(IngestPipeline pipeline) {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=?", pipeline.getId()) == 1;
    }
}
