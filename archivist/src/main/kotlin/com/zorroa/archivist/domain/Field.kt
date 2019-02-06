package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import java.util.*

enum class AttrType {
    STRING,
    STRING_EXACT,
    KEYWORDS,
    CONTENT,
    INTEGER,
    DECIMAL;

    fun fieldName(num: Int) : String {
        return "custom.${name}__$num".toLowerCase()
    }

}

/**
 *
 */
class FieldSpec(
        val name: String,
        var attrName: String?,
        var attrType: AttrType?,
        val editable: Boolean,
        @JsonIgnore var custom: Boolean=false)


class Field (
        val id: UUID,
        val organizationId: UUID,
        val name: String,
        val attrName: String,
        val attrType: AttrType,
        val editable: Boolean,
        val custom: Boolean
)
{
    companion object {

        object TypeRefKList : TypeReference<KPagedList<Field>>()
    }
}

class FieldFilter (
        val ids : List<UUID>? = null,
        val attrTypes: List<AttrType>? = null,
        val attrNames: List<String>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("id" to "field.pk_field",
                    "name" to "field.str_name",
                    "attrType" to "field.int_attr_type",
                    "attrName" to "field.int_attr_name")

    override fun build() {

        if (sort == null) {
            sort = listOf("name:a")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("field.pk_field", it.size))
            addToValues(it)
        }

        attrTypes?.let {
            addToWhere(JdbcUtils.inClause("field.int_attr_type", it.size))
            addToValues(it.map { i-> i.ordinal })
        }

        attrNames?.let {
            addToWhere(JdbcUtils.inClause("field.str_attr_name", it.size))
            addToValues(it)
        }

        addToWhere("field.pk_organization=?")
        addToValues(getOrgId())
    }

}

