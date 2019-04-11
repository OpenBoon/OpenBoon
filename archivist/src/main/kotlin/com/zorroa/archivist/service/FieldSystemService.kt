package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.FieldDao
import com.zorroa.archivist.repository.FieldEditDao
import com.zorroa.archivist.repository.FieldSetDao
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface FieldSystemService {

    fun createField(spec: FieldSpec, reindexSuggest:Boolean=true) : Field
    fun getField(id: UUID) : Field
    fun getField(spec: FieldEditSpec) : Field
    fun getField(attrName: String) : Field
    fun findOneField(filter: FieldFilter): Field
    fun deleteField(field: Field) : Boolean
    fun updateField(field: Field, spec: FieldUpdateSpec): Boolean
    fun getAllFields(filter: FieldFilter) : KPagedList<Field>
    fun getFieldEdits(filter: FieldEditFilter) : KPagedList<FieldEdit>
    fun getFieldEdits(assetId: UUID) : List<FieldEdit>
    fun getFieldEdit(editId: UUID) : FieldEdit
    fun getKeywordFieldNames() : Map<String, Float>
    fun getSuggestAttrNames() : List<String>

    fun createFieldSet(spec: FieldSetSpec, regenSuggest:Boolean=true) : FieldSet
    fun getAllFieldSets(filter: FieldSetFilter) : KPagedList<FieldSet>
    fun getAllFieldSets() : List<FieldSet>
    fun getAllFieldSets(doc: Document) : List<FieldSet>
    fun getFieldSet(id: UUID) : FieldSet

    fun applyFieldEdits(doc: Document)
    fun applySuggestions(doc: Document)

    fun getEsTypeMap(): Map<String, AttrType>
    fun getEsMapping() : Map<String, Any?>
    fun getEsAttrType(attrName: String): AttrType?

    fun setupDefaultFieldSets(org: Organization)
}

@Service
@Transactional
class FieldSystemServiceImpl @Autowired constructor(
        val fieldDao: FieldDao,
        val fieldEditDao: FieldEditDao,
        val fieldSetDao: FieldSetDao,
        val indexRoutingService: IndexRoutingService,
        val properties: ApplicationProperties,
        val txevent: TransactionEventManager
): FieldSystemService {

    @Autowired
    lateinit var jobService: JobService

    override fun createField(spec: FieldSpec, reindexSuggest:Boolean) : Field {

        when {
            spec.attrName != null -> {
                /*
                 * The user has provided a field name, so the type should be known
                 */
                val attrName = spec.attrName as String
                spec.attrType = spec.attrType ?: getEsAttrType(attrName)
            }
            spec.attrType != null -> {
                /*
                 * The user has provided a type but no attribute name, so it's a dynamic
                 * field.
                 */
                spec.attrName = fieldDao.allocate(spec.attrType!!)
                spec.custom = true

            }
            else -> throw IllegalStateException(
                    "Invalid FieldSpec, not enough information to createField a field.")
        }

        if (spec.suggest && reindexSuggest) {
            txevent.afterCommit(sync=false) {
                indexRoutingService.launchReindexJob()
            }
        }
        return fieldDao.create(spec)
    }

    @Transactional(readOnly=true)
    override fun getFieldEdit(editId: UUID) : FieldEdit {
        return fieldEditDao.get(editId)
    }

    @Transactional(readOnly=true)
    override fun getFieldEdits(assetId: UUID) : List<FieldEdit> {
        return fieldEditDao.getAll(assetId)
    }

    @Transactional(readOnly=true)
    override fun getFieldEdits(filter: FieldEditFilter) : KPagedList<FieldEdit> {
        return fieldEditDao.getAll(filter)
    }

    @Transactional(readOnly=true)
    override fun getField(id: UUID) : Field {
        return fieldDao.get(id)
    }

    @Transactional(readOnly=true)
    override fun getAllFields(filter: FieldFilter) : KPagedList<Field> {
        return fieldDao.getAll(filter)
    }

    @Transactional(readOnly=true)
    override fun getField(attrName: String) : Field {
        return fieldDao.get(attrName)
    }

    @Transactional(readOnly=true)
    override fun findOneField(filter: FieldFilter) : Field {
        return fieldDao.findOne(filter)
    }

    override fun deleteField(field: Field): Boolean {
        return fieldDao.delete(field)
    }

    override fun updateField(field: Field, spec: FieldUpdateSpec): Boolean {
        val updated = fieldDao.update(field, spec)
        /**
         * If the row was updated and spec.sugggest is not the same as field.suggest
         * then kick off regenerating suggestions.
         */
        if (updated && spec.suggest != field.suggest) {
            txevent.afterCommit(sync=false) {
                indexRoutingService.launchReindexJob()
            }
        }
        return updated
    }

    @Transactional(readOnly=true)
    override fun getKeywordFieldNames() : Map<String, Float> {
        return fieldDao.getKeywordFieldNames()
    }

    @Transactional(readOnly=true)
    override fun getSuggestAttrNames() : List<String> {
        return fieldDao.getSuggestAttrNames()
    }

    @Transactional(readOnly=true)
    override fun getField(spec: FieldEditSpec) : Field {
        return when {
            spec.fieldId != null -> fieldDao.get(spec.fieldId)
            spec.attrName != null -> fieldDao.get(spec.attrName)
            else -> throw IllegalArgumentException("Must provide a fieldId or AttrName")
        }
    }

    override fun createFieldSet(spec: FieldSetSpec, regenSuggest: Boolean) : FieldSet {
        val fieldIds = spec.fieldSpecs?.mapNotNull { fs->
            val field = fs.attrName?.let {
                val field = if (fieldDao.exists(it)) {
                    getField(it)

                }
                else {
                    createField(fs, regenSuggest)
                }
                field.id
            }
            field
        }
        spec.fieldIds = fieldIds
        return fieldSetDao.create(spec)
    }

    @Transactional(readOnly=true)
    override fun getFieldSet(id: UUID) : FieldSet {
        return fieldSetDao.get(id)
    }

    @Transactional(readOnly=true)
    override fun getAllFieldSets() : List<FieldSet> {
        return fieldSetDao.getAll()
    }

    @Transactional(readOnly=true)
    override fun getAllFieldSets(filter: FieldSetFilter) : KPagedList<FieldSet> {
        return fieldSetDao.getAll(filter)
    }

    @Transactional(readOnly=true)
    override fun getAllFieldSets(doc: Document) : List<FieldSet> {
        return fieldSetDao.getAll(doc)
    }

    @Transactional(readOnly=true)
    override fun applyFieldEdits(doc: Document) {
        fieldEditDao.getAssetUpdateMap(UUID.fromString(doc.id)).forEach { t, u ->
            doc.setAttr(t, u)
        }
    }

    @Transactional(readOnly=true)
    override fun applySuggestions(doc: Document) {
        val values = fieldDao.getSuggestAttrNames().flatMap {
            val attr : Any? = doc.getAttr(it, Any::class.java)
            when (attr) {
                is Collection<*> -> {
                    attr.map { v -> v.toString() }
                }
                else -> {
                    listOf(attr?.toString())
                }
            }
        }.filterNotNull()
        doc.setAttr(SUGGEST_FIELD, values)
    }

    fun createFieldSet(stream: InputStream) {
        val fs = Json.Mapper.readValue<FieldSetSpec>(stream)
        createFieldSet(fs, false)
    }

    override fun getEsMapping() : Map<String, Any> {
        val rest = indexRoutingService.getOrgRestClient()
        val stream = rest.client.lowLevelClient.performRequest(
                "GET", "/${rest.route.indexName}").entity.content
        return  Json.Mapper.readValue(stream, Json.GENERIC_MAP)
    }

    override fun getEsTypeMap(): Map<String, AttrType> {
        val result = mutableMapOf<String, AttrType>()
        val rest = indexRoutingService.getOrgRestClient()
        val map : Map<String, Any> = getEsMapping()
        getMap(result, "", Document(map).getAttr("${rest.route.indexName}.mappings.asset")!!)
        return result
    }

    override fun getEsAttrType(attrName: String): AttrType? {
        return getEsTypeMap()[attrName]
    }

    /**
     * Builds a list of field names, recursively walking each object.
     */
    private fun getMap(result: MutableMap<String, AttrType>,
                        fieldName: String?,
                        mapProperties: Map<String, Any>) {

        if (fieldName == null) {  return }
        val map = mapProperties["properties"] as Map<String, Any>
        for (key in map.keys) {
            val item = map[key] as Map<String, Any>
            if (item.containsKey("type")) {
                var type = item["type"] as String
                var attrType = NAME_TYPE_OVERRRIDES.getOrDefault(type, AttrType.StringAnalyzed)

                if ("analyzer" in item) {
                    attrType = ANALYZER_OVERRIDES.getOrDefault(item["analyzer"] as String, attrType)
                }
                else if ("fields" in item) {
                    val subFields = item["fields"] as Map<String, Any>
                    if (subFields.containsKey("paths")) {
                        attrType = AttrType.StringPath
                    }
                }

                val fqfn ="$fieldName$key"
                result[fqfn] = attrType

            } else {
                getMap(result, arrayOf(fieldName, key, ".").joinToString(""), item)
            }
        }
    }

    override fun setupDefaultFieldSets(org: Organization) {

        val home = properties.getString("archivist.path.home")
        val searchPath = listOf("classpath:/fieldsets", "$home/config/fieldsets")
        val resolver = PathMatchingResourcePatternResolver(javaClass.classLoader)

        searchPath.forEach {
            if (it.startsWith("classpath:")) {
                val resources = resolver.getResources("$it/*.json")
                for (resource in resources) {
                    createFieldSet(resource.inputStream)
                }
            } else {
                val path = Paths.get(it.trim())
                if (Files.exists(path)) {
                    for (file in Files.list(path)) {
                        createFieldSet(FileInputStream(file.toFile()))
                    }
                }
            }
        }
    }

    companion object {

        /**
         * The name of the field used to aggregate suggest fields into a single value.
         */
       const val SUGGEST_FIELD = "system.suggestions"

        /**
         * Maps a particular type or column configuration to our own set of attribute types.
         */
        private val NAME_TYPE_OVERRRIDES = mapOf("shash" to AttrType.HashSimilarity,
                        "long" to AttrType.NumberInteger,
                        "integer" to AttrType.NumberInteger,
                        "double" to AttrType.NumberFloat,
                        "float" to AttrType.NumberFloat,
                        "geo_point" to AttrType.GeoPoint,
                        "boolean" to AttrType.Bool,
                        "date" to AttrType.DateTime,
                        "text" to AttrType.StringAnalyzed,
                        "keyword" to AttrType.StringExact)


        /**
         * Maps a particular ES analyzer to a type of column
         */
        private val ANALYZER_OVERRIDES = mapOf("content" to AttrType.StringContent)

        private val logger = LoggerFactory.getLogger(FieldSystemServiceImpl::class.java)
    }
}