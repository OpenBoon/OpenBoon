package com.zorroa.archivist.repository;

import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.sdk.domain.Analyst;
import com.zorroa.archivist.sdk.domain.AnalystPing;
import com.zorroa.archivist.sdk.domain.AnalystState;
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
            "str_host",
            "int_port",
            "int_threads_total",
            "int_threads_active",
            "int_process_success",
            "int_process_failed",
            "int_queue_size",
            "time_created",
            "time_updated",
            "bool_data");

    @Override
    public Analyst create(AnalystPing ping) {
        long time = System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_analyst"});
            ps.setString(1, ping.getHost());
            ps.setInt(2, ping.getPort());
            ps.setInt(3, ping.getThreadsTotal());
            ps.setInt(4, ping.getThreadsActive());
            ps.setInt(5, ping.getProcessSuccess());
            ps.setInt(6, ping.getProcessFailed());
            ps.setInt(7, ping.getQueueSize());
            ps.setLong(8, time);
            ps.setLong(9, time);
            ps.setBoolean(10, ping.isData());

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
                "time_updated=? "+
            "WHERE " +
                "str_host=? " +
            "AND " +
                "int_port =?";

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
                ping.getHost(),
                ping.getPort()) == 1;
    }

    private static final RowMapper<Analyst> MAPPER = (rs, row) -> {
        Analyst a = new Analyst();
        a.setId(rs.getInt("pk_analyst"));
        a.setHost(rs.getString("str_host"));
        a.setPort(rs.getInt("int_port"));
        a.setData(rs.getBoolean("bool_data"));
        a.setProcessFailed(rs.getInt("int_process_failed"));
        a.setProcessSuccess(rs.getInt("int_process_success"));
        a.setQueueSize(rs.getInt("int_queue_size"));
        a.setState(AnalystState.values()[rs.getInt("int_state")]);
        a.setThreadsActive(rs.getInt("int_threads_active"));
        a.setThreadsTotal(rs.getInt("int_threads_total"));
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
    public List<Analyst> getAll(AnalystState state) {
        return jdbc.query("SELECT * FROM analyst WHERE int_state=?", MAPPER, state.ordinal());
    }

    @Override
    public boolean setState(String host, AnalystState newState, AnalystState oldState) {
        return jdbc.update("UPDATE analyst SET int_state=? WHERE str_host=? AND int_state=?",
                newState.ordinal(), host, oldState.ordinal()) == 1;
    }

    @Override
    public boolean setState(String host, AnalystState newState) {
        return jdbc.update("UPDATE analyst SET int_state=? WHERE str_host=?",
                newState.ordinal(), host) == 1;
    }
}
