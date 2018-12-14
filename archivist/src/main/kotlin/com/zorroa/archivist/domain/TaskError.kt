package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.LongRangeFilter
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

class TaskError(
        val id: UUID,
        val taskId: UUID,
        val jobId: UUID,
        val assetId: UUID?,
        val path: String?,
        val message: String,
        val processor: String,
        val fatal: Boolean,
        val analyst: String,
        val phase: String,
        val timeCreated: Long)


class TaskErrorFilter (
        val ids : List<UUID>? = null,
        val jobIds: List<UUID>? = null,
        val taskIds: List<UUID>? = null,
        val assetIds: List<UUID>? = null,
        val paths: List<String>? = null,
        val processors: List<String>? = null,
        val timeCreated: LongRangeFilter?=null,
        val organizationIds : List<UUID>?=null) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
            "id" to "pk_task_error",
            "taskId" to "pk_task",
            "jobId" to "pk_job",
            "assetId" to "pk_asset",
            "path" to "str_path",
            "processors" to "str_processor",
            "timeCreated" to "time_created")

    @JsonIgnore
    override fun build() {

        ids?.let  {
            addToWhere(JdbcUtils.inClause("task_error.pk_task_error", it.size))
            addToValues(it)
        }

        jobIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_job", it.size))
            addToValues(it)
        }

        taskIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_task", it.size))
            addToValues(it)
        }

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_asset", it.size))
            addToValues(it)
        }

        paths?.let {
            addToWhere(JdbcUtils.inClause("task_error.str_path", it.size))
            addToValues(it)
        }

        processors?.let {
            addToWhere(JdbcUtils.inClause("task_error.str_processor", it.size))
            addToValues(it)
        }

        timeCreated?.let {
            addToWhere(JdbcUtils.rangeClause("task_error.time_created", it))
            addToValues(it.getFilterValues())
        }

        if (hasPermission("zorroa::superadmin")) {
            organizationIds?.let {
                addToWhere(JdbcUtils.inClause("job.pk_organization", it.size))
                addToValues(it)
            }
        }
        else {
            addToWhere("job.pk_organization=?")
            addToValues(getOrgId())
        }

        if (sort == null) {
            sort = listOf("timeCreated:desc")
        }

    }
}