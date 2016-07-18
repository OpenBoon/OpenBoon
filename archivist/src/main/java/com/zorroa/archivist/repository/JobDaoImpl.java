package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.util.Json;
import com.zorroa.sdk.zps.ZpsScript;
import com.zorroa.sdk.zps.ZpsTask;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

import static com.zorroa.archivist.repository.TaskDao.STOPPERS;

/**
 * Created by chambers on 6/24/16.
 */
@Repository
public class JobDaoImpl extends AbstractDao implements JobDao {

    private static final String INSERT =
            JdbcUtils.insert("job",
                    "str_name",
                    "int_type",
                    "int_user_created",
                    "time_started",
                    "json_script");

    @Override
    public ZpsScript create(ZpsScript script, PipelineType type) {
        Preconditions.checkNotNull(script);
        Preconditions.checkNotNull(script.getName());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT, new String[]{"pk_job"});
            ps.setString(1, script.getName());
            ps.setInt(2, type.ordinal());
            ps.setInt(3, SecurityUtils.getUser().getId());
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, Json.serializeToString(script));
            return ps;
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        script.setJobId(id);

        // insert supporting tables.
        jdbc.update("INSERT INTO job_stat (pk_job) VALUES (?)", id);
        jdbc.update("INSERT INTO job_count (pk_job) VALUES (?)", id);
        return script;
    }

    private final RowMapper<Job> MAPPER = (rs, row) -> {
        Job job = new Job();
        job.setId(rs.getInt("pk_job"));
        job.setState(JobState.values()[rs.getInt("int_state")]);
        job.setName(rs.getString("str_name"));
        job.setTimeStarted(rs.getLong("time_started"));
        job.setTimeStopped(rs.getLong("time_stopped"));
        job.setType(PipelineType.values()[rs.getInt("int_type")]);
        job.setUserCreated(resolveUser(rs.getInt("int_user_created")));

        Job.Stats a = new Job.Stats();
        a.setAssetTotal(rs.getInt("int_asset_total_count"));
        a.setAssetCreated(rs.getInt("int_asset_create_count"));
        a.setAssetUpdated(rs.getInt("int_asset_update_count"));
        a.setAssetErrored(rs.getInt("int_asset_error_count"));
        a.setAssetWarning(rs.getInt("int_asset_warning_count"));
        job.setStats(a);

        Job.Counts t = new Job.Counts();
        t.setTasksTotal(rs.getInt("int_task_total_count"));
        t.setTasksCompleted(rs.getInt("int_task_completed_count"));
        t.setTasksWaiting(rs.getInt("int_task_state_waiting_count"));
        t.setTasksQueued(rs.getInt("int_task_state_queued_count"));
        t.setTasksRunning(rs.getInt("int_task_state_running_count"));
        t.setTasksSucess(rs.getInt("int_task_state_success_count"));
        t.setTasksFailure(rs.getInt("int_task_state_failure_count"));
        job.setCounts(t);

        return job;
    };

    private static final String GET =
            "SELECT " +
                "job.pk_job,"+
                "job.str_name,"+
                "job.int_state,"+
                "job.time_started,"+
                "job.time_stopped,"+
                "job.int_type,"+
                "job.int_user_created,"+
                "job_stat.int_asset_total_count,"+
                "job_stat.int_asset_create_count," +
                "job_stat.int_asset_warning_count,"+
                "job_stat.int_asset_error_count,"+
                "job_stat.int_asset_update_count,"+
                "job_count.int_task_total_count,"+
                "job_count.int_task_completed_count,"+
                "job_count.int_task_state_queued_count,"+
                "job_count.int_task_state_waiting_count,"+
                "job_count.int_task_state_running_count,"+
                "job_count.int_task_state_success_count,"+
                "job_count.int_task_state_failure_count " +
            "FROM " +
                "job," +
                "job_count,"+
                "job_stat " +
            "WHERE " +
                "job.pk_job = job_count.pk_job " +
            "AND " +
                "job.pk_job = job_stat.pk_job ";

    @Override
    public Job get(int id) {
        return jdbc.queryForObject(GET.concat("AND job.pk_job=?"), MAPPER, id);
    }

    @Override
    public Job get(ZpsScript script) {
        return get(script.getJobId());
    }

    @Override
    public PagedList<Job> getAll(Paging page, JobFilter filter) {
        Tuple<String, List<Object>> query = filter.getQuery(GET, page);
        return new PagedList<>(page.setTotalCount(count(filter)),
                jdbc.query(query.getLeft(), MAPPER, query.getRight().toArray()));
    }

    private static final String COUNT = "SELECT COUNT(1) FROM job ";

    @Override
    public long count(JobFilter filter) {
        Tuple<String, List<Object>> query = filter.getQuery(COUNT, null);
        return jdbc.queryForObject(query.getLeft(), Long.class, query.getRight().toArray());
    }

    @Override
    public long count() {
        return jdbc.queryForObject(COUNT, Long.class);
    }

    private static final String INC_STATS =
            "UPDATE " +
                "job_stat " +
            "SET " +
                "int_asset_total_count=int_asset_total_count+?,"+
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?,"+
                "int_asset_error_count=int_asset_error_count+?,"+
                "int_asset_update_count=int_asset_update_count+? " +
            "WHERE " +
                "pk_job=?";

    @Override
    public boolean incrementStats(int id, int created, int updated, int errors, int warnings) {
        return jdbc.update(INC_STATS, created+updated+errors, created, warnings, errors, updated, id) == 1;
    }

    private static final String INC_WAITING_TASK_COUNT =
            "UPDATE " +
                "job_count " +
            "SET " +
                "int_task_total_count=int_task_total_count+1,"+
                "int_task_state_waiting_count=int_task_state_waiting_count+1 " +
            "WHERE " +
                "pk_job=?";

    @Override
    public void incrementWaitingTaskCount(ZpsTask task) {
        jdbc.update(INC_WAITING_TASK_COUNT, task.getJobId());
    }

    @Override
    public boolean setState(ZpsTask job, JobState newState, JobState expect) {
        List<Object> values = Lists.newArrayListWithCapacity(4);
        List<String> fields = Lists.newArrayListWithCapacity(4);

        fields.add("int_state=?");
        values.add(newState.ordinal());

        if (newState.equals(JobState.Finished)) {
            fields.add("time_stopped=?");
            values.add(System.currentTimeMillis());
        }

        StringBuilder sb = new StringBuilder(256);
        sb.append("UPDATE job SET ");
        sb.append(String.join(",", fields));
        sb.append(" WHERE pk_job=? ");
        values.add(job.getJobId());
        if (expect != null) {
            values.add(expect.ordinal());
            sb.append(" AND int_state=?");
        }
        return jdbc.update(sb.toString(), values.toArray()) == 1;
    }

    @Override
    public JobState updateTaskStateCounts(ZpsTask task, TaskState newState, TaskState expect) {
        /**
         * TODO: implement as a trigger!
         */
        String p = "int_task_state_" + newState.toString().toLowerCase() + "_count";
        String m = "int_task_state_" + expect.toString().toLowerCase() + "_count";

        List<String> cols = Lists.newArrayList(
                p + "=" + p + "+1",
                m + "=" + m + "-1"
        );

        if (STOPPERS.contains(newState)) {
            cols.add("int_task_completed_count=int_task_completed_count+1");
        }

        String update = new StringBuilder(256)
                .append("UPDATE job_count SET ")
                .append(String.join(",", cols))
                .append(" WHERE pk_job=?").toString();

        if (jdbc.update(update.toString(), task.getJobId()) == 1) {
            if (STOPPERS.contains(newState)) {
                int pt = jdbc.queryForObject(
                        "SELECT int_task_total_count - int_task_completed_count FROM job_count WHERE pk_job=?",
                        Integer.class, task.getJobId());
                if (pt == 0) {
                    if (setState(task, JobState.Finished, JobState.Active)) {
                        return JobState.Finished;
                    }
                }
            }
        }
        else {
            logger.warn("Failed to update task counts for job {}, '{}' '{}'",  task.getJobId(), p, m);
        }

        return JobState.Active;
    }
}
