package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import java.util.*

enum class AttrType {
    STRING,
    ID,
    KEYWORDS,
    CONTENT,
    INTEGER,
    FLOAT,
    DATE;

    fun fieldName(num: Int) : String {
        return "${name}__$num".toLowerCase()
    }

}

class FieldSetSpec(
        val name: String
)

class FieldSet(
        val id: UUID,
        val organizationId: UUID,
        val name: String
)

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

class FieldFilter (
        val ids : List<UUID>? = null,
        val attrTypes: List<AttrType>? = null,
        val attrName: List<AttrType>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf("id" to "field.pk_field")

    override fun build() {

        addToWhere("field.pk_organization=?")
        addToValues(getOrgId())
    }

}

