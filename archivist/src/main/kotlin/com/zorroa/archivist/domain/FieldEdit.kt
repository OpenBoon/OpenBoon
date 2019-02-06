package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import java.util.*

class FieldEditSpecInternal(
        val assetId: UUID,
        val fieldId: UUID,
        val newValue: Any?,
        var oldValue: Any?=null
)

/**
 *
 */
class FieldEditSpec(
        val fieldId: UUID?,
        val attrName: String?,
        val newValue: Any?
)

class FieldEdit(
        val id: UUID,
        val fieldId: UUID,
        val assetId: UUID,
        val oldValue: Any?,
        val newValue: Any?,
        val timeCreated: Long,
        val userCreated: UserBase
)
{
    companion object {

        object TypeRefKList : TypeReference<KPagedList<FieldEdit>>()
    }
}

class FieldEditFilter (
        val ids : List<UUID>? = null,
        val assetIds: List<UUID>? = null,
        val fieldIds: List<UUID>? = null,
        val userCreatedIds: List<UUID>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("id" to "field_edit.pk_field_edit",
                    "assetId" to "field_edit.pk_asset",
                    "fieldId" to "field_edit.pk_field",
                    "timeCreated" to "field_edit.time_created")

    override fun build() {

        if (sort == null) {
            sort = listOf("timeCreated:a")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("field_edit.pk_field_edit", it.size))
            addToValues(it)
        }

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("field_edit.pk_asset", it.size))
            addToValues(it)
        }

        fieldIds?.let {
            addToWhere(JdbcUtils.inClause("field_edit.pk_field", it.size))
            addToValues(it)
        }

        fieldIds?.let {
            addToWhere(JdbcUtils.inClause("field_edit.pk_field", it.size))
            addToValues(it)
        }

        userCreatedIds?.let {
            addToWhere(JdbcUtils.inClause("field_edit.pk_user_created", it.size))
            addToValues(it)
        }

        addToWhere("field_edit.pk_organization=?")
        addToValues(getOrgId())
    }

}


