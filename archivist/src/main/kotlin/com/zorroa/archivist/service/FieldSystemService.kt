package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.FieldEditDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.zorroa.archivist.repository.FieldDao
import com.zorroa.common.repository.KPagedList
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.*

interface FieldSystemService {
    fun create(spec: FieldSpec) : Field
    fun get(id: UUID) : Field
    fun get(spec: FieldEditSpec) : Field
    fun getAll(filter: FieldFilter) : KPagedList<Field>
    fun getFieldEdits(filter: FieldEditFilter) : KPagedList<FieldEdit>
    fun getFieldEdits(assetId: UUID) : List<FieldEdit>
    fun getFieldEdit(editId: UUID) : FieldEdit
}

@Service
@Transactional
class FieldSystemServiceImpl @Autowired constructor(
        val fieldDao: FieldDao,
        val fieldEditDao: FieldEditDao


): FieldSystemService {

    @Autowired
    lateinit var fieldService: FieldService

    override fun create(spec: FieldSpec) : Field {

        when {
            spec.attrName != null -> {
                /*
                 * The user has provided a field name, so the type should be known
                 */
                val attrName = spec.attrName as String
                val esType = fieldService.getFieldType(attrName)
                Preconditions.checkNotNull(esType,
                        "Unable to find attribute in index: $attrName")
                spec.attrType = when (esType) {
                    "string" -> AttrType.STRING
                    "integer" -> AttrType.INTEGER
                    "long" -> AttrType.INTEGER
                    "id" -> AttrType.STRING_EXACT
                    else -> AttrType.STRING
                }
            }
            spec.attrType != null -> {
                /*
                 * The user has provided a type but no attribtue name, so it's a dynamic
                 * field.
                 */
                spec.attrName = fieldDao.allocate(spec.attrType!!)
                spec.custom = true

            }
            else -> throw IllegalStateException("Invalid FieldSpec, not enough information to create a field.")
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

    override fun get(id: UUID) : Field {
        return fieldDao.get(id)
    }

    override fun getAll(filter: FieldFilter) : KPagedList<Field> {
        return fieldDao.getAll(filter)
    }

    override fun get(spec: FieldEditSpec) : Field {
        return when {
            spec.fieldId != null -> fieldDao.get(spec.fieldId)
            spec.attrName != null -> fieldDao.get(spec.attrName)
            else -> throw IllegalArgumentException("Must provide a fieldId or AttrName")
        }
    }
}