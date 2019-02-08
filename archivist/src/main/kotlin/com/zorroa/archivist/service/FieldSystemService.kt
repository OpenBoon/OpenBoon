package com.zorroa.archivist.service

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.base.Preconditions
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
    fun createField(spec: FieldSpec) : Field
    fun getField(id: UUID) : Field
    fun getField(spec: FieldEditSpec) : Field
    fun getField(attrName: String) : Field
    fun getAllFields(filter: FieldFilter) : KPagedList<Field>
    fun getFieldEdits(filter: FieldEditFilter) : KPagedList<FieldEdit>
    fun getFieldEdits(assetId: UUID) : List<FieldEdit>
    fun getFieldEdit(editId: UUID) : FieldEdit

    fun createFieldSet(spec: FieldSetSpec) : FieldSet
    fun getAllFieldSets(filter: FieldSetFilter) : KPagedList<FieldSet>
    fun getAllFieldSets() : List<FieldSet>
    fun getAllFieldSets(doc: Document) : List<FieldSet>
    fun getFieldSet(id: UUID) : FieldSet

    fun applyFieldEdits(doc: Document)

    fun setupDefaultFieldSets(org: Organization)
}

@Service
@Transactional
class FieldSystemServiceImpl @Autowired constructor(
        val fieldDao: FieldDao,
        val fieldEditDao: FieldEditDao,
        val fieldSetDao: FieldSetDao,
        val properties: ApplicationProperties
): FieldSystemService {

    @Autowired
    lateinit var fieldService: FieldService

    override fun createField(spec: FieldSpec) : Field {

        when {
            spec.attrName != null -> {
                /*
                 * The user has provided a field name, so the type should be known
                 */
                val attrName = spec.attrName as String
                spec.attrType = spec.attrType ?: detectAttrType(attrName)
            }
            spec.attrType != null -> {
                /*
                 * The user has provided a type but no attribtue name, so it's a dynamic
                 * field.
                 */
                spec.attrName = fieldDao.allocate(spec.attrType!!)
                spec.custom = true

            }
            else -> throw IllegalStateException("Invalid FieldSpec, not enough information to createField a field.")
        }

        fieldService.invalidateFields()
        return fieldDao.create(spec)
    }

    override fun getFieldEdit(editId: UUID) : FieldEdit {
        return fieldEditDao.get(editId)
    }

    override fun getFieldEdits(assetId: UUID) : List<FieldEdit> {
        return fieldEditDao.getAll(assetId)
    }

    override fun getFieldEdits(filter: FieldEditFilter) : KPagedList<FieldEdit> {
        return fieldEditDao.getAll(filter)
    }

    override fun getField(id: UUID) : Field {
        return fieldDao.get(id)
    }

    override fun getAllFields(filter: FieldFilter) : KPagedList<Field> {
        return fieldDao.getAll(filter)
    }

    override fun getField(attrName: String) : Field {
        return fieldDao.get(attrName)
    }

    override fun getField(spec: FieldEditSpec) : Field {
        return when {
            spec.fieldId != null -> fieldDao.get(spec.fieldId)
            spec.attrName != null -> fieldDao.get(spec.attrName)
            else -> throw IllegalArgumentException("Must provide a fieldId or AttrName")
        }
    }

    override fun createFieldSet(spec: FieldSetSpec) : FieldSet {
        val fieldIds = spec.fieldSpecs?.mapNotNull { fs->
            val field = fs.attrName?.let {
                val field = if (fieldDao.exists(it)) {
                    getField(it)

                }
                else {
                    createField(fs)
                }
                field.id
            }
            field
        }
        spec.fieldIds = fieldIds
        return fieldSetDao.create(spec)
    }

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

    fun detectAttrType(attrName : String) : AttrType {
        val esType = fieldService.getFieldType(attrName)

        Preconditions.checkNotNull(esType,
                "Unable to find attribute in index: $attrName")

        /**
         * These ES types needs to be mapped to the ZVI type.
         * TODO: add Keywords and other fields
         */
        return when (esType) {
            "string" -> AttrType.String
            "integer" -> AttrType.NumberInteger
            "long" -> AttrType.NumberInteger
            "id" -> AttrType.StringExact
            else -> AttrType.String
        }
    }

    fun createFieldSet(stream: InputStream) {
        val fs = Json.Mapper.readValue<FieldSetSpec>(stream)
        createFieldSet(fs)
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

        private val logger = LoggerFactory.getLogger(FieldSystemServiceImpl::class.java)
    }
}