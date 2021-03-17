package boonai.archivist.service

import boonai.archivist.domain.Field
import boonai.archivist.domain.FieldFilter
import boonai.archivist.domain.FieldSpec
import boonai.archivist.repository.CustomFieldDao
import boonai.archivist.repository.FieldDao
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.getZmlpActor
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.security.getProjectId
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

    /**
     * Find a single field.
     */
    fun findOneField(filter: FieldFilter): Field

    /**
     * Search for fields.
     */
    fun findFields(filter: FieldFilter): KPagedList<Field>
}

@Service
@Transactional
class FieldServiceImpl(
    val fieldDao: FieldDao,
    val customFieldDao: CustomFieldDao,
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

    @Transactional(readOnly = true)
    override fun findOneField(filter: FieldFilter): Field {
        return customFieldDao.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun findFields(filter: FieldFilter): KPagedList<Field> {
        return customFieldDao.getAll(filter)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FieldServiceImpl::class.java)
    }
}
