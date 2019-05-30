package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
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
    fun isValid(value: Any?): Boolean {
        if (value == null) {
            return true
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
 */
open class BaseFieldSpec {
    var editable: Boolean = false
    var keywords: Boolean = false
    var keywordsBoost: Float = 1.0f
    var suggest: Boolean = false
    var options: List<Any>? = null
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

/**
 * The properties required to expose an existing ES attribute.
 *
 * @property name The name of the field, aka the label.
 * @property attrName The ES attribute name.
 * @property attrType The type of attribute.
 * @property forceType Forces the attrType value to be used. This is only done internally.
 */
class FieldSpecExpose(
    val name: String,
    var attrName: String,
    var attrType: AttrType? = null,
    @JsonIgnore
    var forceType: Boolean = false
) : BaseFieldSpec()

/**
 * A Field describes the display properties for a given ES attribute.  Each ES attribute
 * can be exposed as a Field, which defines what users can see.
 *
 * @property id The UUID of the field.
 * @property name The name of the field, aka the label.
 * @property attrName The ES attribute name.
 * @property attrType The type of attribute.
 * @property editable If the field is editable or not.
 * @property keywords Set to true if this field should be considered a keyword.
 * @property keywordsBoost The keywords boost level for the field.
 * @property value The value of the field, if the field is resolved against an asset.
 * @property fieldEditId Will be the unique ID of an edit, if the field has been edited on a given asset.
 */
class Field(
    val id: UUID,
    val name: String,
    val attrName: String,
    val attrType: AttrType,
    val editable: Boolean,
    val custom: Boolean,
    val keywords: Boolean,
    val keywordsBoost: Float,
    val suggest: Boolean,
    val options: List<Any>? = null,
    val value: Any? = null,
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
class FieldUpdateSpec(
    val name: String,
    val editable: Boolean,
    var keywords: Boolean,
    val keywordsBoost: Float,
    val suggest: Boolean,
    val options: List<Any>? = null
)

/**
 * The properties required to create a FieldSet.
 *
 * @property name The name or label of field set.
 * @property linkExpression A query string expression used by the server to determine
 *  if an asset should display a field set.
 * @property fieldIds Unique field ids in the field set. Optional.
 * @property attrNames Alternative to fieldIds, provide a list of attribute names which get added to the field set.
 */
class FieldSetSpec(
    val name: String,
    val linkExpression: String? = null,
    var fieldIds: List<UUID>? = null,
    var attrNames: List<String>? = null
)

/**
 * A FieldSet is a collection of fields grouped together under a name.
 *
 * @property id The unique ID of the field set.
 * @property name The name or label of field set.
 * @property linkExpression A query string expression used by the server to determine
 *  if an asset should display a field set.
 * @property fields The fields in the field set. Only populated in some cases.
 */
class FieldSet(
    val id: UUID,
    val name: String,
    val linkExpression: String? = null,
    var fields: MutableList<Field>? = null
)

class FieldSetFilter(
    val ids: List<UUID>? = null,
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

class FieldFilter(
    val ids: List<UUID>? = null,
    val attrTypes: List<AttrType>? = null,
    val attrNames: List<String>? = null,
    val keywords: Boolean? = null,
    val editable: Boolean? = null,
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
