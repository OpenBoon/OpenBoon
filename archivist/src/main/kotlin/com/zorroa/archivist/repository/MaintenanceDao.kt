package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.ExpiredJob
import com.zorroa.archivist.domain.JobState
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.io.File
import java.util.*

interface MaintenanceDao {

    fun backup(file: File)

    fun getExpiredJobs(expireTime: Long): List<ExpiredJob>

    fun removeTaskData(job: ExpiredJob)
}

@Repository
class MaintenanceDaoImpl : AbstractDao(), MaintenanceDao {

    private val JOB_MAPPER = RowMapper<ExpiredJob> { rs, _ ->
        val job = ExpiredJob()
        job.id = rs.getObject("pk_job") as UUID
        job.rootPath = rs.getString("str_root_path")
        job
    }

    override fun backup(file: File) {
        if (isDbVendor("h2")) {
            jdbc.update("BACKUP TO ?", file.absolutePath)
        }
    }

    override fun getExpiredJobs(expireTime: Long): List<ExpiredJob> {
        return jdbc.query(GET_EXPIRED, JOB_MAPPER,
                JobState.Finished.ordinal, JobState.Cancelled.ordinal,
                expireTime)
    }

    override fun removeTaskData(job: ExpiredJob) {
        jdbc.update("DELETE FROM task_stat WHERE pk_job=?", job.id)
        jdbc.update("DELETE FROM task WHERE pk_job=?", job.id)
    }

    companion object {

        private val GET_EXPIRED = "SELECT " +
                "job.pk_job, " +
                "job.str_root_path " +
                "FROM " +
                "job," +
                "job_count " +
                "WHERE " +
                "job.pk_job = job_count.pk_job " +
                "AND " +
                "job.int_state IN (?,?) " +
                "AND " +
                "job_count.time_updated < ? "
    }

}
