package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.domain.AnalystState
import com.zorroa.common.domain.LockState
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

class ProcessorSpec(
    val className: String,
    val type: String,
    val file: String,
    val fileTypes: List<String>?,
    val display: List<Map<String, Any>>?
)

class Processor (
    val id: UUID,
    val className: String,
    val type: String,
    val file: String,
    val fileTypes: List<String>,
    val display: List<Map<String, Any>>,
    val updatedTime: Long
)

class ProcessorFilter(
        val ids : List<UUID>? = null,
        val types : List<String>? = null,
        val classNames: List<String>? = null,
        var fileTypes: List<String>? = null,
        var keywords: String? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
            "id" to "processor.pk_processor",
            "className" to "processor.str_name",
            "type" to "processor.str_type")

    override fun build() {

        if (sort == null) {
            sort = listOf("className:a")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("processor.pk_processor", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("processor.str_type", it.size))
            addToValues(it)
        }

        classNames?.let {
            addToWhere(JdbcUtils.inClause("processor.str_name", it.size))
            addToValues(it)
        }

        fileTypes?.let {
            addToWhere(JdbcUtils.arrayOverlapClause("list_file_types", "text", it.size))
            addToValues(it)
        }

        keywords?.let {
            addToWhere("fti_keywords @@ to_tsquery(?)")
            addToValues(it)
        }

    }

}