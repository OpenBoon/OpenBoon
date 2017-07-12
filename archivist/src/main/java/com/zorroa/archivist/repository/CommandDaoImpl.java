package com.zorroa.archivist.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Command;
import com.zorroa.archivist.domain.CommandSpec;
import com.zorroa.archivist.domain.CommandType;
import com.zorroa.archivist.domain.JobState;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 4/21/17.
 */
@Repository
public class CommandDaoImpl extends AbstractDao implements CommandDao {


    @Autowired
    UserDaoCache userDaoCache;

    private final RowMapper<Command> MAPPER = (rs, row) -> {
        Command c = new Command();
        c.setId(rs.getInt("pk_command"));
        c.setType(CommandType.values()[rs.getInt("int_type")]);
        c.setArgs(Json.deserialize(rs.getString("json_args"),
                new TypeReference<List<Object>>(){}));
        c.setState(JobState.values()[rs.getInt("int_state")]);
        c.setUser(userDaoCache.getUser(rs.getInt("pk_user")));
        c.setTotalCount(rs.getLong("int_total_count"));
        c.setSuccessCount(rs.getLong("int_success_count"));
        c.setErrorCount(rs.getLong("int_error_count"));
        c.setMessage(rs.getString("str_message"));

        long startTime = rs.getLong("time_started");
        if (startTime > 0) {
            long stopTime = rs.getLong("time_stopped");
            if (stopTime <= 0) {
                stopTime = System.currentTimeMillis();
            }
            c.setDuration(Math.max(0, stopTime - startTime));
        }
        return c;
    };

    private static final String INSERT =
            JdbcUtils.insert("command",
                    "pk_user",
                    "time_created",
                    "int_type",
                    "int_state",
                    "json_args");

    @Override
    public Command create(CommandSpec spec) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_command"});
            ps.setInt(1, SecurityUtils.getUser().getId());
            ps.setLong(2, System.currentTimeMillis());
            ps.setInt(3, spec.getType().ordinal());
            ps.setInt(4, JobState.Waiting.ordinal());
            ps.setString(5, Json.serializeToString(spec.getArgs(), "[]"));
            return ps;
        }, keyHolder);

        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    private static final String GET = "SELECT * FROM command ";

    @Override
    public Command get(int id) {
        return jdbc.queryForObject(GET.concat("WHERE pk_command=?"), MAPPER, id);
    }

    @Override
    public List<Command> getPendingByUser() {
        return jdbc.query(GET.concat(
                "WHERE pk_user=? AND int_state IN (?,?) ORDER BY int_state, pk_command"), MAPPER,
                SecurityUtils.getUser().getId(), JobState.Active.ordinal(), JobState.Waiting.ordinal());
    }

    @Override
    public Command refresh(Command object) {
        return get(object.getId());
    }

    @Override
    public List<Command> getAll() {
        return null;
    }

    @Override
    public PagedList<Command> getAll(Pager paging) {
        return null;
    }

    @Override
    public boolean update(int id, Command spec) {
        return false;
    }

    @Override
    public boolean delete(int id) {
        return false;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public Command getNext() {
        try {
            return jdbc.queryForObject(
                    GET.concat("WHERE int_state=? ORDER BY time_created ASC LIMIT 1"),
                    MAPPER, JobState.Waiting.ordinal());
        } catch (EmptyResultDataAccessException e) {
            // just return null;
            return null;
        }
    }

    @Override
    public boolean start(Command cmd) {
        return jdbc.update("UPDATE command SET time_started=?, int_state=? WHERE pk_command=? AND int_state=?",
                System.currentTimeMillis(), JobState.Active.ordinal(), cmd.getId(), JobState.Waiting.ordinal()) > 0;
    }

    @Override
    public boolean stop(Command cmd, String msg) {
        return jdbc.update("UPDATE command SET time_stopped=?, int_state=?, str_message=? WHERE pk_command=? AND int_state=?",
                System.currentTimeMillis(), JobState.Finished.ordinal(), msg, cmd.getId(), JobState.Active.ordinal()) > 0;
    }

    @Override
    public boolean cancel(Command cmd, String msg) {
        return jdbc.update("UPDATE command SET time_stopped=?, int_state=?, str_message=? WHERE pk_command=?",
                System.currentTimeMillis(), JobState.Cancelled.ordinal(), msg, cmd.getId()) > 0;
    }

    private static final String UPDATE_PROGRESS =
            "UPDATE " +
                "command " +
            "SET " +
                "int_total_count=?,"+
                "int_success_count=int_success_count+?," +
                "int_error_count=int_error_count+? " +
            "WHERE " +
                "pk_command=?";

    @Override
    public boolean updateProgress(Command cmd, long total, long success, long error) {
        return jdbc.update(UPDATE_PROGRESS, total, success, error, cmd.getId()) == 1;
    }
}
