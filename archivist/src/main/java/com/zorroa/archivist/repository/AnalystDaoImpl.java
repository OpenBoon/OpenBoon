package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.sdk.domain.Analyst;
import com.zorroa.sdk.domain.AnalystPing;
import com.zorroa.sdk.domain.AnalystState;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 2/10/16.
 */
@Repository
public class AnalystDaoImpl extends AbstractDao implements AnalystDao {

    private static final String INSERT = JdbcUtils.insert("analyst",
            "str_url",
            "int_threads_total",
            "int_threads_active",
            "int_process_success",
            "int_process_failed",
            "int_queue_size",
            "time_created",
            "time_updated",
            "bool_data",
            "json_ingestor_classes");

    @Override
    public Analyst create(AnalystPing ping) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_analyst"});
            ps.setString(1, ping.getUrl());
            ps.setInt(2, ping.getThreadsTotal());
            ps.setInt(3, ping.getThreadsActive());
            ps.setInt(4, ping.getProcessSuccess());
            ps.setInt(5, ping.getProcessFailed());
            ps.setInt(6, ping.getQueueSize());
            ps.setLong(7, time);
            ps.setLong(8, time);
            ps.setBoolean(9, ping.isData());
            ps.setString(10, Json.serializeToString(ping.getIngestProcessorClasses()));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private static final String UPDATE =
            "UPDATE " +
                "analyst " +
            "SET " +
                "int_threads_total=?,"+
                "int_threads_active=?,"+
                "int_process_success=?,"+
                "int_process_failed=?,"+
                "int_queue_size=?,"+
                "bool_data=?, " +
                "time_updated=?, "+
                "json_ingestor_classes=? " +
            "WHERE " +
                "str_url=?";

    @Override
    public boolean update(AnalystPing ping) {
        long time = System.currentTimeMillis();
        return jdbc.update(UPDATE,
                ping.getThreadsTotal(),
                ping.getThreadsActive(),
                ping.getProcessSuccess(),
                ping.getProcessFailed(),
                ping.getQueueSize(),
                ping.isData(),
                time,
                Json.serializeToString(ping.getIngestProcessorClasses()),
                ping.getUrl()) == 1;
    }

    private static final RowMapper<Analyst> MAPPER = (rs, row) -> {
        Analyst a = new Analyst();
        a.setId(rs.getInt("pk_analyst"));
        a.setUrl(rs.getString("str_url"));
        a.setData(rs.getBoolean("bool_data"));
        a.setProcessFailed(rs.getInt("int_process_failed"));
        a.setProcessSuccess(rs.getInt("int_process_success"));
        a.setQueueSize(rs.getInt("int_queue_size"));
        a.setState(AnalystState.values()[rs.getInt("int_state")]);
        a.setThreadsActive(rs.getInt("int_threads_active"));
        a.setThreadsTotal(rs.getInt("int_threads_total"));
        a.setIngestProcessorClasses(Json.deserialize(rs.getString("json_ingestor_classes"), Json.LIST_OF_STRINGS));
        return a;
    };

    @Override
    public Analyst get(int id) {
        return jdbc.queryForObject("SELECT * FROM analyst WHERE pk_analyst=?", MAPPER, id);
    }

    @Override
    public List<Analyst> getAll() {
        return jdbc.query("SELECT * FROM analyst", MAPPER);
    }

    @Override
    public List<Analyst> getActive() {
        return jdbc.query("SELECT * FROM analyst WHERE int_state=?" , MAPPER, AnalystState.UP.ordinal());
    }

    @Override
    public List<Analyst> getAll(AnalystState state) {
        return jdbc.query("SELECT * FROM analyst WHERE int_state=?", MAPPER, state.ordinal());
    }

    @Override
    public boolean setState(String url, AnalystState newState, AnalystState oldState) {
        return jdbc.update("UPDATE analyst SET int_state=? WHERE str_url=? AND int_state=?",
                newState.ordinal(), url, oldState.ordinal()) == 1;
    }

    @Override
    public boolean setState(String url, AnalystState newState) {
        return jdbc.update("UPDATE analyst SET int_state=? WHERE str_url=?",
                newState.ordinal(), url) == 1;
    }
}
