package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.ProjectQuotaCounters
import com.zorroa.archivist.domain.ProjectQuotas
import com.zorroa.archivist.domain.ProjectQuotasTimeSeriesEntry
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.toHourlyDate
import org.springframework.jdbc.`object`.BatchSqlUpdate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Types
import java.util.Calendar
import java.util.Date
import java.util.UUID

interface ProjectQuotasDao {
    fun createQuotasEntry(projectId: UUID)
    fun getQuotas(projectId: UUID): ProjectQuotas
    fun createIngestTimeSeriesEntries(projectId: UUID)
    fun incrementQuotas(counts: ProjectQuotaCounters)
    fun incrementTimeSeriesCounters(date: Date, counts: ProjectQuotaCounters)
    fun getTimeSeriesCounters(projectId: UUID, start: Date, end: Date?): List<ProjectQuotasTimeSeriesEntry>
}

@Repository
class ProjectQuotasDaoImpl : AbstractDao(), ProjectQuotasDao {

    override fun createQuotasEntry(projectId: UUID) {
        jdbc.update("INSERT INTO project_quota (pk_project) VALUES (?)", projectId)
    }

    override fun createIngestTimeSeriesEntries(projectId: UUID) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.DAY_OF_YEAR, 1)

        val update = BatchSqlUpdate(
            jdbc.dataSource,
            "INSERT INTO project_quota_time_series VALUES (?, ?, ?)",
            intArrayOf(Types.OTHER, Types.INTEGER, Types.BIGINT), 250
        )

        for (i in 1..8760) {
            update.update(projectId, i, cal.timeInMillis)
            cal.add(Calendar.HOUR, 1)
        }
        update.flush()
    }

    override fun incrementQuotas(counts: ProjectQuotaCounters) {
        jdbc.update(
            UPDATE_QUOTAS,
            counts.videoLength,
            counts.pageCount,
            getProjectId()
        )
    }

    override fun getQuotas(projectId: UUID): ProjectQuotas {
        return jdbc.queryForObject("SELECT * FROM project_quota WHERE pk_project=?",
            MAPPER_QUOTA, projectId)
    }

    override fun getTimeSeriesCounters(projectId: UUID, start: Date, end: Date?): List<ProjectQuotasTimeSeriesEntry> {
        return jdbc.query("SELECT * FROM project_quota_time_series WHERE " +
            "time >=? AND time <=? AND pk_project=? ORDER BY time ASC",
            MAPPER_TIME_SERIES, toHourlyDate(start), toHourlyDate(end), projectId)
    }

    override fun incrementTimeSeriesCounters(date: Date, counts: ProjectQuotaCounters) {
        val cal: Calendar = Calendar.getInstance()
        cal.timeInMillis = date.time
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val entry = ((cal.get(Calendar.DAY_OF_YEAR) - 1) * 24) + cal.get(Calendar.HOUR_OF_DAY)
        logger.warn("Updating TimeSeriesCounters $entry ${cal.timeInMillis}")
        jdbc.update(
            UPDATE_TIMESCALE_COUNTERS,
            cal.timeInMillis,
            counts.videoFileCount,
            counts.documentFileCount,
            counts.imageFileCount,
            counts.videoLength,
            counts.pageCount,
            counts.videoClipCount,
            getProjectId(),
            entry
        )
    }

    companion object {

        private val MAPPER_QUOTA = RowMapper { rs, _ ->
            ProjectQuotas(
                rs.getLong("int_max_video_seconds"),
                rs.getBigDecimal("float_video_seconds"),
                rs.getLong("int_max_page_count"),
                rs.getLong("int_page_count"))
        }

        private val MAPPER_TIME_SERIES = RowMapper { rs, _ ->
            ProjectQuotasTimeSeriesEntry(
                rs.getLong("time"),
                rs.getBigDecimal("float_video_seconds"),
                rs.getLong("int_page_count"),
                rs.getLong("int_video_file_count"),
                rs.getLong("int_document_file_count"),
                rs.getLong("int_image_file_count"),
                rs.getLong("int_video_clip_count")
            )
        }

        const val UPDATE_QUOTAS = "UPDATE " +
            "project_quota SET " +
            "float_video_seconds=float_video_seconds+?," +
            "int_page_count=int_page_count+? " +
            "WHERE pk_project=?"

        const val UPDATE_TIMESCALE_COUNTERS =
            "UPDATE project_quota_time_series " +
                "SET " +
                "time=?," +
                "int_video_file_count=int_video_file_count+?," +
                "int_document_file_count=int_document_file_count+?," +
                "int_image_file_count=int_image_file_count+?," +
                "float_video_seconds=float_video_seconds+?," +
                "int_page_count=int_page_count+?," +
                "int_video_clip_count=int_video_clip_count+? " +
                "WHERE " +
                "pk_project=? AND int_entry=?"
    }
}
