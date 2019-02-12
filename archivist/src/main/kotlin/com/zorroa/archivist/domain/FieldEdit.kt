package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import java.util.*

/**
 * An internal use only FieldEditSpec
 *
 * @property assetId The ID of the asset to add an edit to.
 * @property fieldId The ID of the field to add an edit to.
 * @property newValue The new field value
 * @property oldValue The old value if any.
 */
class FieldEditSpecInternal(
        val assetId: UUID,
        val fieldId: UUID,
        val newValue: Any?,
        var oldValue: Any?=null
)

/**
 * A FieldEditSpec defines the properties needed to create a FieldEdit
 * with the REST API.
 *
 * This class allows you to specify either a fieldId or attrName.  FieldId is preferred.
 *
 * @property fieldId The ID of the field to add an edit to.
 * @property attrName The attribute name. Optional.
 * @property newValue The new value of the field.
 */
class FieldEditSpec(
        val fieldId: UUID?,
        val attrName: String?,
        val newValue: Any?
)

/**
 * A FieldEdit entry describes manual edit to a specific attribute.
 *
 * @property id The unique Id of the field edit.
 * @property fieldId The unique Id of the field that was edited.
 * @property assetId The unique Id of the asset that was edited.
 * @property oldValue The old value or null if there was no value.
 * @property newValue The new value or null if the edit was to remove the value.
 * @property timeCreated The time the edit was created.
 * @property userCreated The user that created the edit.
 *
 */
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


