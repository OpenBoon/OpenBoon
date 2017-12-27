package com.zorroa.archivist.repository;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.zorroa.archivist.JdbcUtils;
import com.zorroa.archivist.domain.*;
import com.zorroa.archivist.security.SecurityUtils;
import com.zorroa.common.domain.TaskState;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.domain.Tuple;
import com.zorroa.sdk.processor.PipelineType;
import com.zorroa.sdk.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Created by chambers on 6/24/16.
 */
@Repository
public class JobDaoImpl extends AbstractDao implements JobDao {

    @Autowired
    UserDaoCache userDaoCache;

    private static final String INSERT =
            JdbcUtils.insert("job",
                    "pk_job",
                    "str_name",
                    "int_type",
                    "int_user_created",
                    "time_started",
                    "json_args",
                    "json_env",
                    "str_root_path");

    @Override
    public Job create(JobSpec spec) {
        Preconditions.checkNotNull(spec);
        Preconditions.checkNotNull(spec.getName());
        Preconditions.checkNotNull(spec.getType());
        long time = System.currentTimeMillis();

        nextId(spec);

        jdbc.update(connection -> {
            PreparedStatement ps =
                    connection.prepareStatement(INSERT);
            ps.setInt(1, spec.getJobId());
            ps.setString(2, spec.getName());
            ps.setInt(3, spec.getType().ordinal());
            ps.setInt(4, SecurityUtils.getUser().getId());
            ps.setLong(5, time);
            ps.setString(6, Json.serializeToString(spec.getArgs(), "{}"));
            ps.setString(7, Json.serializeToString(spec.getEnv(), "{}"));
            ps.setString(8, spec.getRootPath());
            return ps;
        });

        // insert supporting tables.
        jdbc.update("INSERT INTO job_stat (pk_job) VALUES (?)", spec.getJobId());
        jdbc.update("INSERT INTO job_count (pk_job, time_updated) VALUES (?, ?)", spec.getJobId(), time);
        return get(spec.getJobId());
    }

    @Override
    public JobSpec nextId(JobSpec spec) {
        if (spec.getJobId() == null) {
            if (isDbVendor("postgresql")) {
                spec.setJobId(jdbc.queryForObject("SELECT nextval('zorroa.job_pk_job_seq')", Integer.class));
            }
            else {
                spec.setJobId(jdbc.queryForObject("SELECT JOB_SEQ.nextval FROM dual", Integer.class));
            }
        }
        return spec;
    }

    private final RowMapper<Job> MAPPER = (rs, row) -> {
        Job job = new Job();
        job.setId(rs.getInt("pk_job"));
        job.setName(rs.getString("str_name"));
        job.setTimeStarted(rs.getLong("time_started"));
        job.setTimeUpdated(rs.getLong("time_updated"));
        job.setType(PipelineType.fromObject(rs.getInt("int_type")));
        job.setUser(userDaoCache.getUser(rs.getInt("int_user_created")));
        job.setArgs(Json.deserialize(rs.getString("json_args"), Json.GENERIC_MAP));
        job.setRootPath(rs.getString("str_root_path"));

        AssetStats a = new AssetStats();
        a.setAssetTotalCount(rs.getInt("int_asset_total_count"));
        a.setAssetCreatedCount(rs.getInt("int_asset_create_count"));
        a.setAssetReplacedCount(rs.getInt("int_asset_replace_count"));
        a.setAssetErrorCount(rs.getInt("int_asset_error_count"));
        a.setAssetWarningCount(rs.getInt("int_asset_warning_count"));
        a.setAssetUpdatedCount(rs.getInt("int_asset_update_count"));
        job.setStats(a);

        Job.Counts t = new Job.Counts();
        t.setTasksTotal(rs.getInt("int_task_total_count"));
        t.setTasksCompleted(rs.getInt("int_task_completed_count"));
        t.setTasksWaiting(rs.getInt("int_task_state_waiting_count"));
        t.setTasksQueued(rs.getInt("int_task_state_queued_count"));
        t.setTasksRunning(rs.getInt("int_task_state_running_count"));
        t.setTasksSuccess(rs.getInt("int_task_state_success_count"));
        t.setTasksFailure(rs.getInt("int_task_state_failure_count"));
        t.setTasksSkipped(rs.getInt("int_task_state_skipped_count"));
        job.setCounts(t);

        JobState state = JobState.values()[rs.getInt("int_state")];
        job.setState(state);

        return job;
    };

    private static final String GET =
            "SELECT " +
                "job.pk_job,"+
                "job.str_name,"+
                "job.int_state,"+
                "job.time_started,"+
                "job.int_type,"+
                "job.json_args,"+
                "job.int_user_created,"+
                "job.str_root_path,"+
                "job_stat.int_asset_total_count,"+
                "job_stat.int_asset_create_count," +
                "job_stat.int_asset_replace_count,"+
                "job_stat.int_asset_error_count,"+
                "job_stat.int_asset_warning_count,"+
                "job_stat.int_asset_update_count,"+
                "job_count.int_task_total_count,"+
                "job_count.int_task_completed_count,"+
                "job_count.int_task_state_queued_count,"+
                "job_count.int_task_state_waiting_count,"+
                "job_count.int_task_state_running_count,"+
                "job_count.int_task_state_success_count,"+
                "job_count.int_task_state_failure_count, " +
                "job_count.int_task_state_skipped_count, " +
                "job_count.time_updated " +
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
    public Job get(JobId job) {
        return get(job.getJobId());
    }

    @Override
    public PagedList<Job> getAll(Pager page, JobFilter filter) {
        if (filter == null) {
            filter = new JobFilter();
        }
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
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_update_count=int_asset_update_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?,"+
                "int_asset_replace_count=int_asset_replace_count+?, "+
                "int_asset_total_count=int_asset_total_count+? "+
            "WHERE " +
                "pk_job=?";

    @Override
    public boolean incrementStats(int id, TaskStatsAdder adder) {
        return jdbc.update(INC_STATS,  adder.create, adder.update,
                adder.warning, adder.error, adder.replace, adder.total, id) == 1;
    }

    private static final String DEC_STATS =
            "UPDATE " +
                "job_stat " +
            "SET " +
                "int_asset_create_count=int_asset_create_count-?," +
                "int_asset_update_count=int_asset_update_count-?," +
                "int_asset_warning_count=int_asset_warning_count-?," +
                "int_asset_error_count=int_asset_error_count-?,"+
                "int_asset_replace_count=int_asset_replace_count-? "+
            "WHERE " +
                "pk_job=?";

    @Override
    public boolean decrementStats(int id, AssetStats adder) {
        return jdbc.update(DEC_STATS,
                adder.getAssetCreatedCount(),
                adder.getAssetUpdatedCount(),
                adder.getAssetWarningCount(),
                adder.getAssetErrorCount(),
                adder.getAssetReplacedCount(), id) == 1;
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
    public void incrementWaitingTaskCount(JobId job) {
        jdbc.update(INC_WAITING_TASK_COUNT, job.getJobId());
    }

    @Override
    public boolean setState(JobId job, JobState newState, JobState expect) {
        List<Object> values = Lists.newArrayListWithCapacity(4);
        List<String> fields = Lists.newArrayListWithCapacity(4);

        fields.add("int_state=?");
        values.add(newState.ordinal());

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
    public JobState getState(int id) {
        if (jdbc.queryForObject("SELECT int_task_total_count - (int_task_state_success_count + int_task_state_skipped_count) AS c FROM job_count WHERE pk_job=?",
                Integer.class, id) == 0) {
            return JobState.Finished;
        }
        else {
            return JobState.Active;
        }
    }

    @Override
    public String getRootPath(int id) {
        return jdbc.queryForObject("SELECT str_root_path FROM job WHERE pk_job=?", String.class, id);
    }

    @Override
    public void updateTaskStateCounts(TaskId task, TaskState newState, TaskState expect) {
        if (newState.equals(expect)) {
            return;
        }
        /**
         * TODO: implement as a trigger!
         */
        String p = "int_task_state_" + newState.toString().toLowerCase() + "_count";
        String m = "int_task_state_" + expect.toString().toLowerCase() + "_count";

        List<String> cols = Lists.newArrayList(
                p + "=" + p + "+1",
                m + "=" + m + "-1"
        );


        String update = new StringBuilder(256)
                .append("UPDATE job_count SET ")
                .append(String.join(",", cols))
                .append(" WHERE pk_job=?").toString();

        jdbc.update(update.toString(), task.getJobId());
    }
}
