package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.repository.FieldDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.security.getProjectId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface FieldService {
    /**
     * Create a new field.
     */
    fun createField(spec: FieldSpec): Field

    /**
     * Get an existing field by id.
     */
    fun getField(id: UUID): Field
}

@Service
@Transactional
class FieldServiceImpl(
    val fieldDao: FieldDao,
    val indexMappingService: IndexMappingService
) : FieldService {

    override fun createField(spec: FieldSpec): Field {
        if (spec.type !in Field.ALLOWED_TYPES) {
            throw IllegalArgumentException("The field type ${spec.type} is not currently supported.")
        }

        if (!Field.isValidFieldName(spec.name)) {
            throw IllegalArgumentException("Field names must be alpha-numeric, underscores/dashes are allowed.")
        }

        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()
        val id = UUIDGen.uuid1.generate()

        val field = Field(
            id,
            getProjectId(),
            spec.name,
            spec.type,
            time, time,
            actor, actor
        )

        fieldDao.saveAndFlush(field)
        indexMappingService.addFieldToIndex(field)

        logger.event(
            LogObject.FIELD, LogAction.CREATE,
            mapOf(
                "fieldId" to id,
                "fieldName" to field.name
            )
        )

        return field
    }

    @Transactional(readOnly = true)
    override fun getField(id: UUID): Field {
        return fieldDao.getByProjectIdAndId(getProjectId(), id)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FieldServiceImpl::class.java)
    }
}
