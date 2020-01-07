package com.zorroa.archivist.service

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.repository.PipelineModDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getZmlpActor
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID


interface PipelineModService {
    fun create(spec: PipelineModSpec): PipelineMod
    fun get(id: UUID): PipelineMod
    fun get(name: String): PipelineMod
    fun update(id: UUID, update: PipelineModUpdate): PipelineMod
    fun delete(id: UUID)
}

@Service
@Transactional
class PipelineModServiceImpl(
    val pipelineMod: PipelineModDao
) : PipelineModService {

    @Transactional(readOnly = true)
    override fun get(id: UUID): PipelineMod = pipelineMod.getOne(id)

    @Transactional(readOnly = true)
    override fun get(name: String): PipelineMod {
        return pipelineMod.getByName(name) ?: throw EmptyResultDataAccessException(
            "PipelineModule '$name' not found", 1
        )
    }

    override fun create(spec: PipelineModSpec): PipelineMod {

        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()

        val mod = PipelineMod(
            id,
            spec.name,
            spec.description,
            spec.restricted,
            spec.ops,
            time, time, actor, actor
        )

        val created = pipelineMod.save(mod)
        logger.event(
            LogObject.PIPELINE_MODULE, LogAction.CREATE,
            mapOf(
                "newPipelineModuleId" to created.id,
                "newPipelineModuleName" to created.name
            )
        )
        return created
    }

    override fun update(id: UUID, update: PipelineModUpdate): PipelineMod {
        return get(id).getUpdated(update)
    }

    override fun delete(id: UUID) {
        val mod = get(id)
        pipelineMod.delete(mod)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineModServiceImpl::class.java)
    }
}