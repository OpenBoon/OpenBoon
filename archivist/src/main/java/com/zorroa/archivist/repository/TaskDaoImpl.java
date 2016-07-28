package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.Task;
import com.zorroa.archivist.domain.TaskState;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

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
                "int_state",
                "json_script",
                "int_order",
                "str_execute",
                "time_created",
                "time_state_change");


    private static final Map<String, Integer> ORDER = ImmutableMap.of(
            "generate", 0,
            "pipeline", 100);

    @Override
    public ZpsScript create(ZpsScript script) {
        Integer parent = script.getTaskId();
        long time = System.currentTimeMillis();
        /**
         * TODO: because we insert to get the ID, the ID stored on the script
         * is inaccurate.  Currently we just handle this in the mapper
         * but we could manually query the sequence
         */
        Preconditions.checkNotNull(script.getJobId());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_task"});
            ps.setInt(1, script.getJobId());
            ps.setObject(2, parent);
            ps.setInt(3, TaskState.Waiting.ordinal());
            ps.setString(4, Json.serializeToString(script));
            ps.setInt(5, ORDER.getOrDefault(script.getExecute(), 10000));
            ps.setString(6, script.getExecute());
            ps.setLong(7, time);
            ps.setLong(8, time);
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        /**
         * Replace the task ID with the new task ID before returning.
         */
        script.setTaskId(id);
        return script;
    }

    @Override
    public boolean setHost(ZpsTask task, String host) {
        return jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", host, task.getTaskId()) == 1;
    }

    @Override
    public boolean setExitStatus(ZpsTask task, int exitStatus) {
        return jdbc.update("UPDATE task SET int_exit_status=? WHERE pk_task=?",
                exitStatus, task.getTaskId()) == 1;
    }

    @Override
    public boolean setState(ZpsTask task, TaskState value, TaskState expect) {
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

    private static final RowMapper<ZpsTask> TASK_MAPPER = (rs, row) -> {
        final int taskId = rs.getInt(1);
        final int jobId = rs.getInt(2);
        return new ZpsTask() {
            public Integer getJobId() { return jobId; }
            public Integer getTaskId() { return taskId; }
        };
    };

    private static final RowMapper<ZpsScript> ZPS_MAPPER = (rs, row) ->
            Json.deserialize(rs.getString(1), ZpsScript.class)
                .setTaskId(rs.getInt(2))
                .setJobId(rs.getInt(3));

    private static final String GET_WAITING =
            "SELECT " +
                "task.json_script,"+
                "task.pk_task,"+
                "task.pk_job " +
            "FROM " +
                "task,"+
                "job " +
            "WHERE " +
                "task.pk_job = job.pk_job " +
            "AND " +
                "job.int_state = 0 " +
            "AND " +
                "task.int_state = 0 " +
            "ORDER BY " +
                "task.int_order ASC LIMIT ? ";
    @Override
    public List<ZpsScript> getWaiting(int limit) {
        return jdbc.query(GET_WAITING, ZPS_MAPPER, limit);
    }

    private static final String GET_QUEUED =
            "SELECT " +
                "task.pk_task,"+
                "task.pk_job " +
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
    public List<ZpsTask> getOrphanTasks(int limit, long duration, TimeUnit unit) {
        return jdbc.query(GET_QUEUED, TASK_MAPPER,
                System.currentTimeMillis() - unit.toMillis(duration), limit);
    }

    private static final RowMapper<Task> MAPPER = (rs, row) -> {
        Task task = new Task();
        task.setTaskId(rs.getInt("pk_task"));
        task.setJobId(rs.getInt("pk_job"));
        task.setParentId(rs.getInt("pk_parent"));
        task.setExitStatus(rs.getInt("int_exit_status"));
        task.setExecute(rs.getString("str_execute"));
        task.setHost(rs.getString("str_host"));
        task.setScript(rs.getString("json_script"));
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
            "task.int_state,"+
            "task.time_started,"+
            "task.time_stopped,"+
            "task.time_created,"+
            "task.time_state_change,"+
            "task.json_script,"+
            "task.str_execute,"+
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
}
