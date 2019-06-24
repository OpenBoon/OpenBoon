package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.Date
import java.util.UUID

/**
 * The valid Attribute Types for the field system.
 */
enum class AttrType(val prefix: String, val editable: kotlin.Boolean) {
    StringAnalyzed("string_analyzed", true),
    StringExact("string_exact", true),
    @Deprecated("Using this type simply marks the field as both keyword and suggest")
    StringSuggest("string_suggest", true),
    StringContent("string_content", true),
    StringPath("string_path", true),
    NumberInteger("number_integer", true),
    NumberFloat("number_float", true),
    Bool("boolean", true),
    DateTime("date_time", true),
    GeoPoint("geo_point", false),
    HashSimilarity("hash_similarity", false);

    /**
     * Return the name of the custom file name.
     */
    fun getCustomAttrName(num: Int): kotlin.String {
        return "custom.${prefix}__$num".toLowerCase()
    }

    /**
     * Check a value against this AttrType to make sure ES won't reject it.  Null always
     * returns true
     *
     * @param value the value to check.
     * @return True if the value is ok, otherwise false.
     */
    fun isValid(obj: Any?): Boolean {
        if (obj == null) {
            return true
        }

        val value = if (obj is Collection<*>) {
            if (obj.isEmpty()) {
                return true
            } else {
                obj.first()
            }
        } else {
            obj
        }

        return when (this) {
            NumberInteger -> {
                value is Int || value is Long
            }
            NumberFloat -> {
                value is Double || value is Float
            }
            StringExact,
            StringContent,
            StringAnalyzed,
            StringSuggest,
            StringPath -> {
                value is CharSequence
            }
            Bool -> {
                value is Boolean
            }
            HashSimilarity -> {
                value is String
            }
            DateTime -> {
                value is Long || value is String || value is Date
            }
            GeoPoint -> {
                value is List<*>
            }
        }
    }
}

/**
 * The base class for FieldSpec, FieldSpecCustom and FieldSpecExpose.
 *
 * @property editable If the field is editable or not.
 * @property keywords Set to true if this field should be considered a keyword.
 * @property keywordsBoost The keywords boost level for the field.
 * @property suggest If the field is a suggest field or not.
 * @property options The valid set of options for a field.
 * @property requireList Force the field to only accept a list of values.
 */
open class BaseFieldSpec {
    var editable: Boolean = false
    var keywords: Boolean = false
    var keywordsBoost: Float = 1.0f
    var suggest: Boolean = false
    var options: List<Any>? = null
    var requireList: Boolean = false
}

/**
 * [FieldSpecCustom] and [FieldSpecExpose] converted turned into a [FieldSpec] which
 * is used to create a [Field].  This class is not exposed via the REST API.
 *
 * @property name The label/name of the Field
 * @property attrName The ES attribute name.
 * @property attrType The type of attribute.
 * @property custom True if the Field is a custom attribute.
 */
@ApiModel(value = "Field Spec", description = "Contains everything needed to create a Field.")
class FieldSpec(
    val name: String,
    var attrName: String,
    var attrType: AttrType,
    var custom: Boolean = false
) : BaseFieldSpec() {

    constructor(spec: FieldSpecCustom, attrName: String) : this(spec.name, attrName, spec.attrType) {
        this.editable = spec.editable
        this.keywords = spec.keywords
        this.keywordsBoost = spec.keywordsBoost
        this.suggest = spec.suggest
        this.options = spec.options
        this.custom = true
    }

    constructor(spec: FieldSpecExpose, attrType: AttrType) : this(spec.name, spec.attrName, attrType) {
        this.editable = spec.editable
        this.keywords = spec.keywords
        this.keywordsBoost = spec.keywordsBoost
        this.suggest = spec.suggest
        this.options = spec.options
        this.custom = false
    }
}

/**
 * The properties required to create a new [Field] that points at a custom ES attribute.
 *
 * @property name The name of the field, aka the label.
 * @property attrType The type of attribute.
 */
class FieldSpecCustom(
    val name: String,
    var attrType: AttrType
) : BaseFieldSpec()

@ApiModel("Field Spec Expose", description = "Properties required to expose an existing ES attribute.")
class FieldSpecExpose(

    @ApiModelProperty("Name/label of the Field.")
    val name: String,

    @ApiModelProperty("ES attribute name.")
    var attrName: String,

    @ApiModelProperty("Type of the attribute.")
    var attrType: AttrType? = null,

    @JsonIgnore
    @ApiModelProperty("Forces the attrType value to be used. This is only done internally.")
    var forceType: Boolean = false

) : BaseFieldSpec()

@ApiModel(
    "Field", description = "Field describes the display properties for a given ES attribute. " +
        "Each ES attribute can be exposed as a Field, which defines what users can see."
)
class Field(

    @ApiModelProperty("UUID of the Field.")
    val id: UUID,

    @ApiModelProperty("Display name of the Field.")
    val name: String,

    @ApiModelProperty("ES attribute name.")
    val attrName: String,

    @ApiModelProperty("Type of the Field.")
    val attrType: AttrType,

    @ApiModelProperty("If true this Field can be edited.")
    val editable: Boolean,

    @ApiModelProperty("If true this is a custom user-created Field.")
    val custom: Boolean,

    @ApiModelProperty("If true this Field will get a keywords field and be searchable.")
    val keywords: Boolean,

    @ApiModelProperty("Boost value for the keywords results.")
    val keywordsBoost: Float,

    @ApiModelProperty("If true this Field will show up in the list of suggestions.")
    val suggest: Boolean,

    @ApiModelProperty("The data stored in this field must be a list.")
    val requireList: Boolean,

    @ApiModelProperty("List of valid options for this Field.")
    val options: List<Any>? = null,

    @ApiModelProperty("Current value of this Field, if the field is resolved against an asset..")
    val value: Any? = null,

    @ApiModelProperty("UUID of the Field Edit that set the value.")
    val fieldEditId: UUID? = null
) {
    companion object {

        object TypeRefKList : TypeReference<KPagedList<Field>>()
    }
}

/**
 * A FieldUpdateSpec describes the properties which can updated on a Field.
 *
 * @property name The name of the field, aka the label.
 * @property editable If the field is editable or not.
 * @property keywords Set to true if this field should be considered a keyword.
 * @property keywordsBoost The keywords boost level for the field.
 * @property suggest True the field should drive suggestions.
 * @property options Available options.
 */
@ApiModel("Field Update Spec", description = "Describes the properties which can be updated on a Field.")
class FieldUpdateSpec(

    @ApiModelProperty("Name/label of the Field.")
    val name: String,

    @ApiModelProperty("If true this Field is editable.")
    val editable: Boolean,

    @ApiModelProperty("If true this field is considered a keyword.")
    var keywords: Boolean,

    @ApiModelProperty("Keywords boost level for the field.")
    val keywordsBoost: Float,

    @ApiModelProperty("If true this field will be considered for auto-complete suggestions.")
    val suggest: Boolean,

    @ApiModelProperty("The data stored in this field must be a list.")
    val requireList: Boolean,

    @ApiModelProperty("Available options for this Field's values.")
    val options: List<Any>? = null

)

@ApiModel("Field Set Spec", description = "Properties required to create a FieldSet.")
class FieldSetSpec(

    @ApiModelProperty("Name/label of Field Set.")
    val name: String,

    @ApiModelProperty(
        "Query string expression used by the server to determine if an asset should display a " +
            "field set."
    )
    val linkExpression: String? = null,

    @ApiModelProperty("UUIDs of Fields in this Field Set.")
    var fieldIds: List<UUID>? = null,

    @ApiModelProperty("Alternative to fieldIds, provide a list of attribute names which get added to the field set.")
    var attrNames: List<String>? = null
)

@ApiModel("Field Set", description = "Collection of Fields grouped together under a name.")
class FieldSet(

    @ApiModelProperty("UUID of the Field Set.")
    val id: UUID,

    @ApiModelProperty("Name of the Field Set.")
    val name: String,

    @ApiModelProperty(
        "Query string expression used by the server to determine if an asset should display a " +
            "field set."
    )
    val linkExpression: String? = null,

    @ApiModelProperty("Fields in the Field Set. Only populated in some cases.")
    var fields: MutableList<Field>? = null

)

@ApiModel("Field Set Filter", description = "Search filter for finding Field Sets.")
class FieldSetFilter(

    @ApiModelProperty("Field Set UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Field Set names to match.")
    val names: List<String>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "field_set.pk_field_set",
            "name" to "field_set.str_name"
        )

    override fun build() {

        if (sort == null) {
            sort = listOf("name:a")
        }

        ids?.let {
            addToWhere(JdbcUtils.inClause("field_set.pk_field_set", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("field_set.str_name", it.size))
            addToValues(it)
        }

        addToWhere("field_set.pk_organization=?")
        addToValues(getOrgId())
    }
}

@ApiModel("Field Filter", description = "Search filter finding Fields.")
class FieldFilter(

    @ApiModelProperty("Field UUIDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("Attribute Types to match.")
    val attrTypes: List<AttrType>? = null,

    @ApiModelProperty("Attribute names to match.")
    val attrNames: List<String>? = null,

    @ApiModelProperty("Keywords setting to match.")
    val keywords: Boolean? = null,

    @ApiModelProperty("Editable setting to match.")
    val editable: Boolean? = null,

    @ApiModelProperty("Suggest setting to match.")
    val suggest: Boolean? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "field.pk_field",
            "name" to "field.str_name",
            "attrType" to "field.int_attr_type",
            "attrName" to "field.int_attr_name"
        )

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
            addToValues(it.map { i -> i.ordinal })
        }

        attrNames?.let {
            addToWhere(JdbcUtils.inClause("field.str_attr_name", it.size))
            addToValues(it)
        }

        keywords?.let {
            addToWhere("field.bool_keywords=?")
            addToValues(keywords)
        }

        editable?.let {
            addToWhere("field.bool_editable=?")
            addToValues(editable)
        }

        suggest?.let {
            addToWhere("field.bool_suggest=?")
            addToValues(suggest)
        }

        addToWhere("field.pk_organization=?")
        addToValues(getOrgId())
    }
}
