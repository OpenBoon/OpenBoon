package com.zorroa.archivist.service

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.zorroa.archivist.repository.FieldDao
import java.lang.IllegalStateException

interface FieldSystemService {
    fun create(spec: FieldSpec) : Field
}

@Service
@Transactional
class FieldSystemServiceImpl @Autowired constructor(
        val fieldDao: FieldDao
): FieldSystemService {

    @Autowired
    lateinit var fieldService: FieldService

    override fun create(spec: FieldSpec) : Field {
        if (spec.attrName != null) {
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
                "id" -> AttrType.ID
                else -> AttrType.STRING
            }
        }
        else if (spec.attrType != null) {
            /*
             * The user has provided a type but no attribue name, so it's a dynamic
             * field.
             */
            spec.attrName = fieldDao.allocate(spec.attrType!!)
            spec.custom = true

        }
        else {
            throw IllegalStateException("Invalid FieldSpec, not enough information to create a field.")
        }

        return fieldDao.create(spec)
    }
}