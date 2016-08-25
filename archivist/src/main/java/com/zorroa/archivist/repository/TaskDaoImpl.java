package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.TaskSpec;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.common.domain.ExecuteTaskStart;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.domain.TaskId;
import com.zorroa.sdk.util.Json;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by chambers on 7/11/16.
 */
@Repository
public class TaskDaoImpl extends AbstractDao implements TaskDao {

    private static final String INSERT =
            JdbcUtils.insert("task",
                "pk_job",
                "pk_parent",
                "str_name",
                "int_state",
                "json_script",
                "int_order",
                "time_created",
                "time_state_change");

    @Override
    public Task create(TaskSpec task) {
        long time = System.currentTimeMillis();
        /**
         * TODO: because we insert to get the ID, the ID stored on the script
         * is inaccurate.  Currently we just handle this in the mapper
         * but we could manually query the sequence
         */
        Preconditions.checkNotNull(task.getJobId());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_task"});
            ps.setInt(1, task.getJobId());
            ps.setObject(2, task.getParentTaskId());
            ps.setString(3, task.getName() == null ? "subtask" : task.getName());
            ps.setInt(4, TaskState.Waiting.ordinal());
            ps.setString(5, task.getScript());
            ps.setInt(6, 1);
            ps.setLong(7, time);
            ps.setLong(8, time);
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        return get(id);
    }

    @Override
    public boolean setHost(TaskId task, String host) {
        return jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", host, task.getTaskId()) == 1;
    }

    @Override
    public boolean setExitStatus(TaskId task, int exitStatus) {
        return jdbc.update("UPDATE task SET int_exit_status=? WHERE pk_task=?",
                exitStatus, task.getTaskId()) == 1;
    }

    @Override
    public boolean setState(TaskId task, TaskState value, TaskState expect) {
        logger.debug("setting task: {} from {} to {}", task.getTaskId(), expect, value);
        List<Object> values = Lists.newArrayListWithCapacity(4);
        List<String> fields = Lists.newArrayListWithCapacity(4);
        long time = System.currentTimeMillis();

        fields.add("int_state=?");
        values.add(value.ordinal());
        fields.add("time_state_change=?");
        values.add(time);

        if (STOPPERS.contains(value)) {
            fields.add("time_stopped=?");
            values.add(time);
        }
        else if (STARTERS.contains(value)) {
            fields.add("time_stopped=-1");
            fields.add("time_started=?");
            values.add(time);
        }
        else if (RESET.contains(value)) {
            fields.add("time_stopped=-1");
            fields.add("time_started=-1");
        }

        values.add(task.getTaskId());

        StringBuilder sb = new StringBuilder(256);
        sb.append("UPDATE task SET ");
        sb.append(String.join(",", fields));
        sb.append(" WHERE pk_task=? ");
        if (expect != null) {
            values.add(expect.ordinal());
            sb.append(" AND int_state=?");
        }

        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    private static final String DECREMENT_DEPEND =
            "UPDATE " +
                "task " +
            "SET " +
                "int_depend_count=int_depend_count-1 " +
            "WHERE " +
                "pk_depend_parent = ? " +
            "AND " +
                "int_depend_count > 0";

    @Override
    public int decrementDependCount(TaskId finishedTask) {
        logger.info("decrementing: task:{} parent:{}", finishedTask.getTaskId(), finishedTask.getParentTaskId());
        // Decrement tasks depending on both ourself and our parent.
        int count = jdbc.update(DECREMENT_DEPEND, finishedTask.getTaskId());
        if (finishedTask.getParentTaskId() != null) {
            count+=jdbc.update(DECREMENT_DEPEND, finishedTask.getParentTaskId());
        }
        return count;
    }

    private static final String INCREMENT_DEPEND =
        "UPDATE " +
            "task " +
        "SET " +
            "int_depend_count=int_depend_count+1 " +
        "WHERE " +
            "pk_depend_parent=?";

    @Override
    public int incrementDependCount(TaskId task) {
        int count = jdbc.update(INCREMENT_DEPEND, task.getTaskId());
        if (task.getParentTaskId() != null) {
            count+=jdbc.update(INCREMENT_DEPEND, task.getParentTaskId());
        }
        return count;
    }

    private static final String SET_DEPEND =
            "UPDATE " +
                "task " +
            "SET " +
                "pk_parent=null,"+
                "int_depend_count=1, " +
                "pk_depend_parent=? "+
            "WHERE " +
                "pk_task=? " +
            "AND " +
                "pk_depend_parent IS NULL";

    @Override
    public boolean createParentDepend(TaskId child) {
        // Might have to check if the parent task is done.
        return jdbc.update(SET_DEPEND, child.getParentTaskId(), child.getTaskId()) > 0;
    }

    private static final RowMapper<Path> LOG_PATH_MAPPER = (rs, row) ->
            Paths.get(
                    rs.getString("str_log_path")).resolve(
                    rs.getString("str_name").replace(' ', '_')
                            .concat(String.format(".%04d.log", rs.getInt("pk_task"))));


    private static final RowMapper<ExecuteTaskStart> EXECUTE_TASK_MAPPER = (rs, row) -> {
        /*
         * We don't parse the script here, its not needed as we're just going to
         * turn it back into a string anyway.
         */
        ExecuteTaskStart e = new ExecuteTaskStart();
        e.setScript(rs.getString(1));
        e.setTaskId(rs.getInt(2));
        e.setJobId(rs.getInt(3));
        if (rs.getObject(4) != null) {
            e.setParentTaskId(rs.getInt(4));
        }
        e.setArgs(Json.deserialize(rs.getString(5), Json.GENERIC_MAP));
        e.setEnv(Json.deserialize(rs.getString(6), Map.class));
        e.setLogPath(LOG_PATH_MAPPER.mapRow(rs, row).toString());
        return e;
    };

    private static final String GET_WAITING =
            "SELECT " +
                "task.json_script,"+
                "task.pk_task,"+
                "task.pk_job, " +
                "task.pk_parent, "+
                "job.json_args,"+
                "job.json_env, " +
                "job.str_log_path, " +
                "task.str_name "+
            "FROM " +
                "task,"+
                "job " +
            "WHERE " +
                "task.pk_job = job.pk_job " +
            "AND " +
                "job.int_state = 0 " +
            "AND " +
                "task.int_state = 0 " +
            "AND " +
                "task.int_depend_count = 0 " +
            "ORDER BY " +
                "task.int_order ASC LIMIT ? ";
    @Override
    public List<ExecuteTaskStart> getWaiting(int limit) {
        return jdbc.query(GET_WAITING, EXECUTE_TASK_MAPPER, limit);
    }

    private static final String GET_QUEUED =
            "SELECT " +
                "task.* " +
            "FROM " +
                "task,"+
                "job " +
            "WHERE " +
                "task.pk_job = job.pk_job " +
            "AND " +
                "job.int_state = 0 " +
            "AND " +
                "task.int_state IN (1, 2) " +
            "AND " +
                "task.time_state_change < ? " +
            "LIMIT ? ";

    @Override
    public List<Task> getOrphanTasks(int limit, long duration, TimeUnit unit) {
        return jdbc.query(GET_QUEUED, MAPPER,
                System.currentTimeMillis() - unit.toMillis(duration), limit);
    }

    private static final RowMapper<Task> MAPPER = (rs, row) -> {
        Task task = new Task();
        task.setTaskId(rs.getInt("pk_task"));
        task.setJobId(rs.getInt("pk_job"));
        task.setParentId(rs.getInt("pk_parent"));
        task.setName(rs.getString("str_name"));
        task.setExitStatus(rs.getInt("int_exit_status"));
        task.setHost(rs.getString("str_host"));
        task.setState(TaskState.values()[rs.getInt("int_state")]);
        task.setTimeCreated(rs.getLong("time_created"));
        task.setTimeStarted(rs.getLong("time_started"));
        task.setTimeStopped(rs.getLong("time_stopped"));
        task.setTimeStateChange(rs.getLong("time_state_change"));
        return task;
    };

    private static final String GET_TASKS =
        "SELECT " +
            "task.pk_task,"+
            "task.pk_parent,"+
            "task.pk_job,"+
            "task.str_name,"+
            "task.int_state,"+
            "task.time_started,"+
            "task.time_stopped,"+
            "task.time_created,"+
            "task.time_state_change,"+
            "task.json_script,"+
            "task.int_exit_status,"+
            "task.str_host " +
        "FROM " +
            "task ";

    @Override
    public PagedList<Task> getAll(int job, Paging page) {
        return new PagedList(page.setTotalCount(countByJob(job)),
                jdbc.query(GET_TASKS.concat("WHERE task.pk_job=? ORDER BY pk_task LIMIT ? OFFSET ?"),
                        MAPPER, job, page.getSize(), page.getFrom()));
    }

    @Override
    public Task get(int id) {
        return jdbc.queryForObject(GET_TASKS.concat("WHERE task.pk_task=?"), MAPPER, id);
    }

    @Override
    public long countByJob(int job) {
        return jdbc.queryForObject("SELECT COUNT(1) FROM task WHERE task.pk_job=?", Long.class, job);
    }
    private static final String GET_LOG_PATH =
        "SELECT " +
            "task.pk_task,"+
            "job.str_log_path, " +
            "task.str_name "+
        "FROM " +
            "task,"+
            "job " +
        "WHERE " +
            "task.pk_job = job.pk_job " +
        "AND " +
            "task.pk_task=?";


    @Override
    public Path getLogFilePath(int id) {
        return jdbc.queryForObject(GET_LOG_PATH, LOG_PATH_MAPPER, id);
    }
}
