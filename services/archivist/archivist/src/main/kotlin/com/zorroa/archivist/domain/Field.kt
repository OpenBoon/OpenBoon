package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

class FieldSpec(

    @ApiModelProperty("The ES field name")
    val name: String,

    @ApiModelProperty("The ES data type.")
    val type: String,
)

@Entity
@Table(name = "field")
@ApiModel("Field", description = "A ZMLP Field")
class Field(

    @Id
    @Column(name = "pk_field")
    @ApiModelProperty("The Unique ID of the field.")
    val id: UUID,

    @Column(name = "pk_project")
    @ApiModelProperty("The Unique ID of the project.")
    val projectId: UUID,

    @ApiModelProperty("The ES field name")
    @Column(name = "str_name")
    val name: String,

    @ApiModelProperty("The ES data type.")
    @Column(name = "str_type")
    val type: String,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Field was created..")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Field was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The actor which created this Field")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The actor that last made the last modification the Field.")
    val actorModified: String,

) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Field) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun getEsField(): String {
        return "custom.$name"
    }

    companion object {

        val NAME_REGEX = Regex("^[a-z0-9_\\-]+$", RegexOption.IGNORE_CASE)

        val ALLOWED_TYPES = setOf(
            "binary",
            "boolean",
            "keyword",
            "fulltext_keyword", // special
            "constant_keyword",
            "wildcard",
            "long",
            "integer",
            "short",
            "byte",
            "double",
            "float",
            "half_float",
            "date",
            "text",
            "geo_point",
            "geo_shape",
            "point",
            "shape"
        )
    }
}
