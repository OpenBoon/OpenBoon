package com.zorroa.archivist.repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.zorroa.archivist.SecurityUtils;

@Repository
public class IngestDaoImpl extends AbstractDao implements IngestDao {

    private static final RowMapper<Ingest> MAPPER = new RowMapper<Ingest>() {
        @Override
        public Ingest mapRow(ResultSet rs, int row) throws SQLException {
            Ingest result = new Ingest();
            result.setId(rs.getLong("pk_ingest"));
            result.setPipelineId(rs.getInt("pk_pipeline"));
            result.setProxyConfigId(rs.getInt("pk_proxy_config"));
            result.setState(IngestState.values()[rs.getInt("int_state")]);
            result.setPath(rs.getString("str_path"));
            result.setTimeCreated(rs.getLong("time_created"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setTimeModified(rs.getLong("time_modified"));
            result.setUserCreated(rs.getString("str_user_created"));
            result.setFileTypes((Set<String>) rs.getObject("list_types"));
            result.setTimeStarted(rs.getLong("time_started"));
            result.setTimeStopped(rs.getLong("time_stopped"));
            result.setCreatedCount(rs.getInt("int_created_count"));
            result.setErrorCount(rs.getInt("int_error_count"));
            return result;
        }
    };

    @Override
    public Ingest get(long id) {
        return jdbc.queryForObject("SELECT * FROM ingest WHERE pk_ingest=?", MAPPER, id);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "ingest " +
            "(" +
                "pk_pipeline,"+
                "pk_proxy_config,"+
                "int_state,"+
                "str_path,"+
                "list_types,"+
                "time_created,"+
                "str_user_created,"+
                "time_modified, "+
                "str_user_modified "+

            ") " +
            "VALUES (" + StringUtils.repeat("?", ",", 9) + ")";

    @Override
    public Ingest create(IngestPipeline pipeline, ProxyConfig proxyConfig, IngestBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_ingest"});
            ps.setInt(1, pipeline.getId());
            ps.setInt(2, proxyConfig.getId());
            ps.setInt(3, IngestState.Idle.ordinal());
            ps.setObject(4, builder.getPath());
            ps.setObject(5, builder.getFileTypes());
            ps.setLong(6, time);
            ps.setString(7, SecurityUtils.getUsername());
            ps.setLong(8, time);
            ps.setString(9, SecurityUtils.getUsername());
            return ps;
        }, keyHolder);
        long id = keyHolder.getKey().longValue();
        return get(id);
    }

    @Override
    public List<Ingest> getAll() {
        return jdbc.query("SELECT * FROM ingest ORDER BY time_created ASC", MAPPER);
    }

    @Override
    public List<Ingest> getAll(IngestState state, int limit) {
        return jdbc.query("SELECT * FROM ingest WHERE int_state =? ORDER BY time_created ASC LIMIT ?",
                MAPPER, state.ordinal(), limit);
    }

    @Override
    public List<Ingest> getAll(IngestFilter filter) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("SELECT * FROM ingest INNER JOIN pipeline ON ingest.pk_pipeline = pipeline.pk_pipeline");

        List<String> wheres = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (filter.getStates() != null) {
            wheres.add(JdbcUtils.in("int_state", filter.getStates().size()));
            filter.getStates().forEach(value -> values.add(value.ordinal()));
        }

        if (filter.getPipelines() != null) {
            wheres.add(JdbcUtils.in("pipeline.str_name", filter.getPipelines().size()));
            values.addAll(filter.getPipelines());
        }

        if (!wheres.isEmpty()) {
            sb.append(" WHERE ");
        }

        sb.append(StringUtils.join(wheres, " AND "));
        if (filter.getLimit() > 0) {
            sb.append(" LIMIT ?");
            values.add(filter.getLimit());
        }

        return jdbc.query(sb.toString(), MAPPER, values.toArray());
    }

    @Override
    public boolean update(Ingest ingest, IngestUpdateBuilder builder) {

        StringBuilder sb = new StringBuilder(512);
        sb.append("UPDATE ingest SET ");

        List<String> updates = Lists.newArrayList();
        List<Object> values = Lists.newArrayList();

        if (builder.getPath() != null) {
            updates.add("str_path=?");
            values.add(builder.getPath());
        }

        if (builder.getFileTypes() != null) {
            updates.add("list_types=?");
            values.add(builder.getFileTypes());
        }

        if (builder.getPipelineId() > 0) {
            updates.add("pk_pipeline=?");
            values.add(builder.getPipelineId());
        }

        if (builder.getProxyConfigId() > 0) {
            updates.add("pk_proxy_config=?");
            values.add(builder.getProxyConfigId());
        }

        if (updates.isEmpty()) {
            return false;
        }

        updates.add("str_user_modified=?");
        values.add(SecurityUtils.getUsername());

        updates.add("time_modified");
        values.add(System.currentTimeMillis());

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_ingest=?");
        values.add(ingest.getId());

        logger.debug("{} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }


    @Override
    public boolean setState(Ingest ingest, IngestState newState, IngestState oldState) {
        return jdbc.update("UPDATE ingest SET int_state=? WHERE pk_ingest=? AND int_state=?",
                    newState.ordinal(), ingest.getId(), oldState.ordinal()) == 1;
    }

    @Override
    public boolean setState(Ingest ingest, IngestState newState) {
        return jdbc.update("UPDATE ingest SET int_state=? WHERE pk_ingest=? AND int_state != ?",
                newState.ordinal(), ingest.getId(), newState.ordinal()) == 1;
    }
}
