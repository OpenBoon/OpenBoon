package com.zorroa.archivist.service

import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.domain.getStandardModules
import com.zorroa.archivist.repository.PipelineModDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.security.withAuth
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException
import java.util.UUID


interface PipelineModService {
    fun create(spec: PipelineModSpec): PipelineMod
    fun get(id: UUID): PipelineMod
    fun get(name: String): PipelineMod
    fun getByNames(names: List<String>): List<PipelineMod>
    fun getByIds(names: List<UUID>): List<PipelineMod>
    fun update(id: UUID, update: PipelineModUpdate): PipelineMod
    fun delete(id: UUID)
    fun updateStandardMods()
}

@Service
@Transactional
class PipelineModServiceImpl(
    val pipelineModDao: PipelineModDao
) : PipelineModService {

    @Transactional(readOnly = true)
    override fun get(id: UUID): PipelineMod = pipelineModDao.getOne(id)

    @Transactional(readOnly = true)
    override fun get(name: String): PipelineMod {
        return pipelineModDao.getByName(name) ?: throw EmptyResultDataAccessException(
            "PipelineModule '$name' not found", 1
        )
    }

    @Transactional(readOnly = true)
    override fun getByIds(names: List<UUID>): List<PipelineMod> {
        val found = pipelineModDao.findByIdIn(names)
        if (found.size != names.size) {
            val missing = names.minus(found.map { it.name })
            throw DataRetrievalFailureException("The Pipeline Mods ${missing.joinToString(",")} do not exist.")
        }
        return found
    }


    @Transactional(readOnly = true)
    override fun getByNames(names: List<String>): List<PipelineMod> {
        val trimmedNames = names.map { it.trim('+', '-')}
        val found = pipelineModDao.findByNameIn(trimmedNames)
        if (found.size != names.size) {
            val missing = trimmedNames.minus(found.map { it.name })
            throw DataRetrievalFailureException("The Pipeline Mods ${missing.joinToString(",")} do not exist.")
        }
        return found
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

        val created = pipelineModDao.save(mod)
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
        logger.event(LogObject.PIPELINE_MODULE, LogAction.UPDATE, mapOf("pipelineModId" to id))
        val updated = get(id).getUpdated(update)
        pipelineModDao.saveAndFlush(updated)
        return updated
    }

    override fun delete(id: UUID) {
        logger.event(LogObject.PIPELINE_MODULE, LogAction.DELETE, mapOf("pipelineModId" to id))
        val mod = get(id)
        pipelineModDao.delete(mod)
    }

    @EventListener(ContextRefreshedEvent::class)
    fun postApplicationStartup() {
        if (ArchivistConfiguration.unittest) {
            return
        }
        updateStandardMods()
    }

    /**
     * Update the standard set of PipelineModules.
     */
    override fun updateStandardMods() {
        logger.info("Updating Standard Pipeline Mods")
        withAuth(InternalThreadAuthentication(KnownKeys.PROJZERO)) {
            for (mod in getStandardModules()) {
                val pmod = pipelineModDao.getByName(mod.name)
                if (pmod == null) {
                    create(mod)
                } else {
                    val update = PipelineModUpdate(
                        pmod.name, pmod.description, pmod.restricted, pmod.ops
                    )
                    update(pmod.id, update)
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineModServiceImpl::class.java)
    }
}