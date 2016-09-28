package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.ExpiredJob;
import com.zorroa.archivist.domain.JobState;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.List;

/**
 * Created by chambers on 4/21/16.
 */
@Repository
public class MaintenanceDaoImpl extends AbstractDao implements MaintenanceDao {

    @Override
    public void backup(File file) {
        jdbc.update("BACKUP TO ?", file.getAbsolutePath());
    }

    private final RowMapper<ExpiredJob> JOB_MAPPER = (rs, row) -> {
        ExpiredJob job = new ExpiredJob();
        job.setId(rs.getInt("pk_job"));
        job.setLogPath(rs.getString("str_log_path"));
        return job;
    };

    private static final String GET_EXPIRED =
            "SELECT " +
                "job.pk_job, " +
                "job.str_log_path " +
            "FROM " +
                "job,"+
                "job_count " +
            "WHERE " +
                "job.pk_job = job_count.pk_job " +
            "AND " +
                "job.int_state IN (?,?) " +
            "AND " +
                "job_count.time_updated < ? ";

    @Override
    public List<ExpiredJob> getExpiredJobs(long expireTime) {
        return jdbc.query(GET_EXPIRED, JOB_MAPPER,
                JobState.Active.ordinal(), JobState.Cancelled.ordinal(),
                expireTime);
    }

    @Override
    public void removeTaskData(ExpiredJob job) {
        jdbc.update("DELETE FROM task_stat WHERE pk_job=?", job.getId());
        jdbc.update("DELETE FROM task WHERE pk_job=?", job.getId());
    }

}
