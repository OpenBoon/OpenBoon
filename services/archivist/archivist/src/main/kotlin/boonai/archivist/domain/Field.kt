package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import boonai.archivist.security.getProjectId
import boonai.archivist.util.JdbcUtils
import com.fasterxml.jackson.annotation.JsonIgnore
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

    fun getPath(): String {
        return "custom.$name"
    }

    companion object {

        fun isValidFieldName(name: String): Boolean {
            return name.matches(NAME_REGEX)
        }

        fun isValidEsFieldName(name: String): Boolean {
            return name.matches(FIELD_REGEX)
        }

        /**
         * A Regex that matches a valid field name.
         */
        val NAME_REGEX = Regex("^[a-z0-9_\\-]{2,32}$", RegexOption.IGNORE_CASE)

        /**
         * A Regex that matches a valid ES field name.
         */
        val FIELD_REGEX = Regex("^custom\\.[A-Za-z0-9_\\-]+$")

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

@ApiModel("Job Filter", description = "Search filter for finding Fields.")
class FieldFilter(
    @ApiModelProperty("Field UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Field names to match.")
    val names: List<String>? = null,

    @ApiModelProperty("Field types to match.")
    val types: List<String>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "field.pk_field",
            "name" to "field.str_name",
            "type" to "field.str_type"
        )

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:desc")
        }

        addToWhere("field.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("field.pk_field", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("field.str_name", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("field.str_type", it.size))
            addToValues(it)
        }
    }
}
