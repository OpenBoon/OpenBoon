package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.security.getUser
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.PipelineType
import com.zorroa.common.domain.ProcessorRef
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.search.AssetSearch
import com.zorroa.common.util.JdbcUtils
import java.util.*

data class Export(
        val id: UUID,
        val organizationId: UUID,
        val userId: UUID,
        val name: String,
        val timeCreated : Long,
        val type: PipelineType,
        val state: JobState
)

/**
 * Defines fields needed to make new ExportFile.
 */
data class ExportFileSpec (
        var path: String,
        val mimeType: String,
        val size: Long
)

/**
 * An ExportFile record.
 */
data class ExportFile (
        val id: UUID,
        val exportId: UUID,
        val name: String,
        val path: String,
        val mimeType : String,
        val size : Long,
        val timeCreated: Long)


/**
 * Defines fields needed to create a new export.
 */
data class ExportSpec (
        var name: String?,
        var search: AssetSearch,
        var processors: List<ProcessorRef> = mutableListOf(),
        var args: Map<String,Any> = mutableMapOf(),
        var env: Map<String,String> = mutableMapOf(),
        var compress: Boolean = true)


/**
 * ExportFilter is used to filter exports by specific fields.
 */
data class ExportFilter (
        private val ids : List<UUID>? = null,
        private val states : List<JobState>? = null
) : KDaoFilter() {

    override val sortMap: Map<String, String>? = null

    @JsonIgnore
    override fun build() {

        if (!ids.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("export.pk_job", ids!!.size))
            addToValues(ids)
        }

        if (!states.orEmpty().isEmpty()) {
            addToWhere(JdbcUtils.inClause("export.int_state", states!!.size))
            addToValues(states.map{it.ordinal})
        }

        val user = getUser()

        addToWhere("pk_user_created=?")
        addToValues(user.id)

        addToWhere("pk_organization=?")
        addToValues(user.organizationId)
    }
}
