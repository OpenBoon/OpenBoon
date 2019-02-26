package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import java.util.*

/**
 * The valid Attribute Types for the field system.
 */
enum class AttrType(val prefix: String, val editable: kotlin.Boolean) {
    StringAnalyzed("string_analyzed", true),
    StringExact("string_exact",true),
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
    fun getCustomAttrName(num: Int) : kotlin.String {
        return "custom.${prefix}__$num".toLowerCase()
    }

    /**
     * Check a value against this AttrType to make sure ES won't reject it.  Null always
     * returns true
     *
     * @param value the value to check.
     * @return True if the value is ok, otherwise false.
     */
    fun isValid(value: Any?) : Boolean {
        if (value == null) {
            return true
        }

        return when(this) {
            AttrType.NumberInteger-> { value is Int || value is Long }
            AttrType.NumberFloat-> { value is Double || value is Float }
            AttrType.StringExact,
            AttrType.StringContent,
            AttrType.StringAnalyzed,
            AttrType.StringSuggest,
            AttrType.StringPath -> { value is CharSequence }
            AttrType.Bool -> { value is Boolean }
            AttrType.HashSimilarity -> { value is String }
            AttrType.DateTime -> { value is Long || value is String || value is Date }
            AttrType.GeoPoint -> { value is List<*> }
        }
    }
}


/**
 * The properties required to create a new field.
 *
 * @property name The name of the field, aka the label.
 * @property attrName The ES attribute name.
 * @property attrType The type of attribute.
 * @property editable If the field is editable or not.
 * @property custom If the field is a custom field or a Zorroa standard.
 * @property keywords Set to true if this field should be considered a keyword.
 * @property keywordsBoost The keywords boost level for the field.
 */
class FieldSpec(
        val name: String,
        var attrName: String?,
        var attrType: AttrType?,
        val editable: Boolean=false,
        val keywords: Boolean=false,
        val keywordsBoost: Float=1.0f,
        @JsonIgnore var custom: Boolean=false)


/**
 * A Field describes the display properties for a given ES attribute.  Each ES attribute
 * can be exposed as a Field, which defines what users can see.
 *
 * @property id The UUID of the field.
 * @property name The name of the field, aka the label.
 * @property attrName The ES attribute name.
 * @property attrType The type of attribute.
 * @property editable If the field is editable or not.
 * @property custom If the field is a custom field or a Zorroa standard.
 * @property keywords Set to true if this field should be considered a keyword.
 * @property keywordsBoost The keywords boost level for the field.
 * @property value The value of the field, if the field is resolved against an asset.
 * @property fieldEditId Will be the unique ID of an edit, if the field has been edited on a given asset.
 */
class Field (
        val id: UUID,
        val name: String,
        val attrName: String,
        val attrType: AttrType,
        val editable: Boolean,
        val custom: Boolean,
        val keywords: Boolean,
        val keywordsBoost: Float,
        val value: Any?=null,
        val fieldEditId: UUID?=null
)
{
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
 */
class FieldUpdateSpec (
        val name: String,
        val editable: Boolean,
        val keywords: Boolean,
        val keywordsBoost: Float
)

/**
 * The properties required to create a FieldSet.
 *
 * @property name The name or label of field set.
 * @property linkExpression A query string expression used by the server to determine
 *  if an asset should display a field set.
 * @property fieldIds Unique field ids in the field set. Optional.
 * @property fieldSpecs A set of field specs which will add fields and then assign them to the field set.
 */
class FieldSetSpec(
        val name: String,
        val linkExpression: String? = null,
        var fieldIds : List<UUID>? = null,
        var fieldSpecs : List<FieldSpec>? = null
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
        val linkExpression: String?=null,
        var fields: MutableList<Field>?=null
) {
    object FieldSetList : TypeReference<List<FieldSet>>()
}

class FieldSetFilter (
    val ids : List<UUID>? = null,
    val names: List<String>? = null
) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("id" to "field_set.pk_field_set",
                    "name" to "field_set.str_name")

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

        addToWhere("field.pk_organization=?")
        addToValues(getOrgId())
    }
}

class FieldFilter (
        val ids : List<UUID>? = null,
        val attrTypes: List<AttrType>? = null,
        val attrNames: List<String>? = null,
        val keywords: Boolean? = null
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

        keywords?.let {
            addToWhere("field.bool_keywords=?")
            addToValues(keywords)
        }

        addToWhere("field.pk_organization=?")
        addToValues(getOrgId())
    }

}

