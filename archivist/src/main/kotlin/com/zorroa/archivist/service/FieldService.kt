package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.HideField
import com.zorroa.archivist.repository.FieldDao
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

interface FieldService {

    fun getQueryFields(): Map<String, Float>

    fun getFields(type: String): Map<String, Set<String>>

    fun getFieldMap(type: String): Map<String, Set<String>>

    fun invalidateFields()

    fun updateField(value: HideField): Boolean

    fun dotRaw(field: String): String

    fun getFieldType(field: String): String?
}

@Service
class FieldServiceImpl @Autowired constructor(
        val indexRoutingService: IndexRoutingService,
        val properties: ApplicationProperties,
        val fieldDao: FieldDao

): FieldService {

    private val fieldMapCache = CacheBuilder.newBuilder()
            .maximumSize(2)
            .initialCapacity(3)
            .concurrencyLevel(1)
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build(object : CacheLoader<String, Map<String, Set<String>>>() {
                @Throws(Exception::class)
                override fun load(key: String): Map<String, Set<String>> {
                    return getFieldMap(key)
                }
            })

    override fun getFieldMap(type: String): Map<String, Set<String>> {
        val hiddenFields = fieldDao.getHiddenFields()
        val result = mutableMapOf<String, MutableSet<String>>()
        result["string"] = mutableSetOf()
        result["date"] = mutableSetOf()
        result["integer"] = mutableSetOf()
        result["long"] = mutableSetOf()
        result["point"] = mutableSetOf()
        result["keywords"] = mutableSetOf()
        result["keywords-boost"] = mutableSetOf()
        result["similarity"] = mutableSetOf()
        result["id"] = mutableSetOf()
        result["path"] = mutableSetOf()

        result.getValue("keywords-boost")
                .addAll(properties.getString(PROP_BOOST_KEYWORD_FIELD)
                        .splitToSequence(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .map { it }
                )

        val rest = indexRoutingService[getOrgId()]
        val stream = rest.client.lowLevelClient.performRequest(
                "GET", "/${rest.route.indexName}").entity.content

        val map : Map<String, Any> = Json.Mapper.readValue(stream, Json.GENERIC_MAP)
        getList(result, "", Document(map).getAttr("${rest.route.indexName}.mappings.asset")!!, hiddenFields)
        return result
    }

    /*
     * TODO: Move all field stuff to asset service.
     */

    override fun invalidateFields() {
        fieldMapCache.invalidateAll()
    }

    override fun updateField(value: HideField): Boolean {
        try {
            return if (value.isHide) {
                fieldDao.hideField(value.field, value.isManual)
            } else {
                fieldDao.unhideField(value.field)
            }
        } finally {
            invalidateFields()
        }
    }

    override fun getFieldType(field: String): String? {
        val fields = getFields("asset")
        for ((k,v) in fields.entries) {
            if (field in v) {
                return k
            }
        }
        return null
    }

    override fun getFields(type: String): Map<String, Set<String>> {
        return try {
            fieldMapCache.get(type)
        } catch (e: Exception) {
            logger.warn("Failed to get fields: ", e)
            ImmutableMap.of()
        }
    }

    override fun dotRaw(field: String): String {
        val idFields = getFieldMap("asset")["id"]

        if (field.endsWith(".raw") && idFields!!.contains(field.removeSuffix(".raw"))) {
            return field.removeSuffix(".raw")
        }

        val strFields = getFieldMap("asset")["string"]
        if (!field.endsWith(".raw") && strFields!!.contains(field)) {
            return "$field.raw"
        }

        return field
    }


    /**
     * Builds a list of field names, recursively walking each object.
     */
    private fun getList(result: MutableMap<String, MutableSet<String>>,
                        fieldName: String?,
                        mapProperties: Map<String, Any>,
                        hiddenFieldNames: Set<String>) {

        if (fieldName == null) {  return }
        val map = mapProperties["properties"] as Map<String, Any>
        for (key in map.keys) {
            val item = map[key] as Map<String, Any>

            if (!fieldName.isEmpty()) {
                if (hiddenFieldNames.contains(fieldName)) {
                    continue
                }
            }

            if (item.containsKey("type")) {
                var type = item["type"] as String
                val analyzer = item["analyzer"] as String?

                type = (NAME_TYPE_OVERRRIDES as java.util.Map<String, String>).getOrDefault(key, type)
                if (type == "text") {
                    type = "string"
                }
                else if (type == "keyword") {
                    type = "id"
                }

                if (item.containsKey("fields")) {
                    val subFields = item["fields"] as Map<String, Any>
                    if (subFields.containsKey("paths")) {
                        type = "path"
                    }
                }

                var fields: MutableSet<String>? = result[type]
                if (fields == null) {
                    fields = TreeSet()
                    result[type] = fields
                }
                val fqfn = arrayOf(fieldName, key).joinToString("")
                if (hiddenFieldNames.contains(fqfn)) {
                    continue
                }
                fields.add(fqfn)

                /**
                 * If the field name is "keywords", then its special!
                 */
                if (key == "keywords") {
                    result.getValue("keywords").add(fqfn)
                }

            } else {
                getList(result, arrayOf(fieldName, key, ".").joinToString(""),
                        item, hiddenFieldNames)
            }
        }
    }

    override fun getQueryFields(): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        val fields = getFields("asset")

        fields.getValue("keywords").forEach { v-> result[v] = 1.0f }
        for (field in fields.getValue("keywords-boost")) {
            val (key,boost) = field.split(':', limit=2)
            try {
                result[key] = boost.toFloat()
            } catch (e: Exception) {
                logger.warn("Failed to parse boosted keyword field: {}", field)
            }
        }

        return result
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FieldServiceImpl::class.java)

        private val NAME_TYPE_OVERRRIDES = ImmutableMap.of(
                "point", "point",
                "shash", "similarity")

        /**
         * The properties prefix used to define keywords fields.
         */
        private const val PROP_BOOST_KEYWORD_FIELD = "archivist.search.keywords.boost"
    }
}

