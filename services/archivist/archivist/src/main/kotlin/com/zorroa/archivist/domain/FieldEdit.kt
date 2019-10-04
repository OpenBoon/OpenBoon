package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

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
    var oldValue: Any? = null
)

@ApiModel("Field Edit Spec", description = "Defines the properties needed to create a FieldEdit. This class " +
    "allows you to specify either a fieldId or attrName. fieldId is preferred.")
class FieldEditSpec(

    @ApiModelProperty("UUID of the Asset that was edited.")
    val assetId: UUID,

    @ApiModelProperty("UUID of the Field that was edited.")
    val fieldId: UUID?,

    @ApiModelProperty("Name of the attribute that was edited.")
    val attrName: String?,

    @ApiModelProperty("New value of the Field.")
    val newValue: Any?

) {
    constructor(assetId: String, fieldId: UUID?, attrName: String?, newValue: Any?) : this(
            UUID.fromString(assetId), fieldId, attrName, newValue)
}

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
) {
    companion object {

        object TypeRefKList : TypeReference<KPagedList<FieldEdit>>()
    }
}

@ApiModel("Field Edit Filter", description = "Search filter for Field Edits.")
class FieldEditFilter(

    @ApiModelProperty("Field Edit UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Asset UUIDs to match.")
    val assetIds: List<UUID>? = null,

    @ApiModelProperty("Field UUIDs to match.")
    val fieldIds: List<UUID>? = null,

    @ApiModelProperty("UUIDs of User's that created Field Edits to match.")
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

        userCreatedIds?.let {
            addToWhere(JdbcUtils.inClause("field_edit.pk_user_created", it.size))
            addToValues(it)
        }

        addToWhere("field_edit.pk_organization=?")
        addToValues(getOrgId())
    }
}
