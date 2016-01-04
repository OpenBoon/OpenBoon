package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.IngestSchedule;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
public class IngestDaoImpl extends AbstractDao implements IngestDao {

    private static final RowMapper<Ingest> MAPPER = (rs, row) -> {
        Ingest result = new Ingest();
        result.setId(rs.getInt("pk_ingest"));
        result.setPipelineId(rs.getInt("pk_pipeline"));
        result.setState(IngestState.values()[rs.getInt("int_state")]);
        result.setPath(rs.getString("str_path"));
        result.setTimeCreated(rs.getLong("time_created"));
        result.setUserCreated(rs.getInt("user_created"));
        result.setTimeModified(rs.getLong("time_modified"));
        result.setUserModified(rs.getInt("user_modified"));
        result.setTimeStarted(rs.getLong("time_started"));
        result.setTimeStopped(rs.getLong("time_stopped"));
        result.setCreatedCount(rs.getInt("int_created_count"));
        result.setErrorCount(rs.getInt("int_error_count"));
        result.setUpdatedCount(rs.getInt("int_updated_count"));
        result.setUpdateOnExist(rs.getBoolean("bool_update_on_exist"));
        result.setAssetWorkerThreads(rs.getInt("int_asset_worker_threads"));
        return result;
    };

    @Override
    public Ingest get(int id) {
        return jdbc.queryForObject("SELECT * FROM ingest WHERE pk_ingest=?", MAPPER, id);
    }

    private static final String INSERT =
            "INSERT INTO " +
                "ingest " +
            "(" +
                "pk_pipeline,"+
                "int_state,"+
                "str_path,"+
                "time_created,"+
                "user_created,"+
                "time_modified, "+
                "user_modified, "+
                "bool_update_on_exist, " +
                "int_asset_worker_threads" +
            ") " +
            "VALUES (" + StringUtils.repeat("?", ",", 9) + ")";

    @Override
    public Ingest create(IngestPipeline pipeline, IngestBuilder builder) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                connection.prepareStatement(INSERT, new String[]{"pk_ingest"});
            ps.setInt(1, pipeline.getId());
            ps.setInt(2, IngestState.Idle.ordinal());
            ps.setObject(3, builder.getPath());
            ps.setLong(4, time);
            ps.setLong(5, SecurityUtils.getUser().getId());
            ps.setLong(6, time);
            ps.setLong(7, SecurityUtils.getUser().getId());
            ps.setBoolean(8, builder.isUpdateOnExist());
            ps.setInt(9, builder.getAssetWorkerThreads());
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
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

        if (builder.getPipelineId() > 0) {
            updates.add("pk_pipeline=?");
            values.add(builder.getPipelineId());
        }

        if (builder.getAssetWorkerThreads() > 0) {
            updates.add("int_asset_worker_threads=?");
            values.add(builder.getAssetWorkerThreads());
        }

        if (updates.isEmpty()) {
            return false;
        }

        updates.add("bool_update_on_exist=?");
        values.add(builder.isUpdateOnExist());

        updates.add("user_modified=?");
        values.add(SecurityUtils.getUser().getId());

        updates.add("time_modified=?");
        values.add(System.currentTimeMillis());

        sb.append(StringUtils.join(updates, ", "));
        sb.append(" WHERE pk_ingest=?");
        values.add(ingest.getId());

        logger.debug("{} {}", sb.toString(), values);
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public boolean delete(Ingest ingest) {
        return jdbc.update("DELETE FROM ingest WHERE pk_ingest=? AND int_state=?",
                ingest.getId(), IngestState.Idle.ordinal()) == 1;
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

    @Override
    public void resetCounters(Ingest ingest) {
        jdbc.update("UPDATE ingest SET " +
                        "int_created_count=0, int_updated_count=0, int_error_count=0 WHERE pk_ingest=?",
                ingest.getId());
    }

    private static final String INCREMENT_COUNTERS =
            "UPDATE " +
                "ingest " +
            "SET " +
                "int_created_count=int_created_count+?," +
                "int_updated_count=int_updated_count+?," +
                "int_error_count=int_error_count+? " +
            "WHERE " +
                "pk_ingest=?";

    @Override
    public void incrementCounters(Ingest ingest, int created, int updated, int errors) {
        jdbc.update(INCREMENT_COUNTERS, created, updated, errors, ingest.getId());
    }

    @Override
    public boolean updateStartTime(Ingest ingest, long time) {
        return jdbc.update("UPDATE ingest SET time_started=?, time_stopped=-1 WHERE pk_ingest=?", time, ingest.getId()) == 1;
    }

    @Override
    public boolean updateStoppedTime(Ingest ingest, long time) {
        return jdbc.update("UPDATE ingest SET time_stopped=? WHERE pk_ingest=?", time, ingest.getId()) == 1;
    }

    @Override
    public List<Ingest> getAll(IngestSchedule schedule) {
        return jdbc.query("SELECT ingest.* FROM ingest,map_schedule_to_ingest m WHERE m.pk_schedule=? AND ingest.pk_ingest = m.pk_ingest",
                MAPPER, schedule.getId());
    }
}
