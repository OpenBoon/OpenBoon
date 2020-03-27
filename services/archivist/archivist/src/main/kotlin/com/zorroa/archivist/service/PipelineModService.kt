package com.zorroa.archivist.service

import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModFilter
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.domain.SupportedMedia
import com.zorroa.archivist.domain.getStandardModules
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.PipelineModCustomDao
import com.zorroa.archivist.repository.PipelineModDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.security.withAuth
import com.zorroa.archivist.util.isUUID
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

interface PipelineModService {
    fun create(spec: PipelineModSpec): PipelineMod
    fun get(id: UUID): PipelineMod
    fun get(name: String): PipelineMod
    fun getByNames(names: Collection<String>): List<PipelineMod>
    fun getByIds(names: Collection<UUID>): List<PipelineMod>
    fun search(filter: PipelineModFilter): KPagedList<PipelineMod>
    fun findOne(filter: PipelineModFilter): PipelineMod
    fun update(id: UUID, update: PipelineModUpdate): PipelineMod
    fun delete(id: UUID)
    fun updateStandardMods()
}

@Service
@Transactional
class PipelineModServiceImpl(
    val pipelineModDao: PipelineModDao,
    val pipelineModCustomDao: PipelineModCustomDao
) : PipelineModService {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Transactional(readOnly = true)
    override fun get(id: UUID): PipelineMod = pipelineModDao.getOne(id)

    @Transactional(readOnly = true)
    override fun get(name: String): PipelineMod {
        return pipelineModDao.getByName(name) ?: throw EmptyResultDataAccessException(
            "PipelineModule '$name' not found", 1
        )
    }

    @Transactional(readOnly = true)
    override fun getByIds(names: Collection<UUID>): List<PipelineMod> {
        val found = pipelineModDao.findByIdIn(names)
        if (found.size != names.size) {
            val missing = names.minus(found.map { it.name })
            throw DataRetrievalFailureException("The Pipeline Mods ${missing.joinToString(",")} do not exist.")
        }
        return found
    }

    @Transactional(readOnly = true)
    override fun getByNames(names: Collection<String>): List<PipelineMod> {
        val trimmedNames = names.map { it.trim('+', '-') }
        val byIds = trimmedNames.filter { it.isUUID() }.map { UUID.fromString(it) }
        val byNames = trimmedNames.filter { !it.isUUID() }

        val namesFound = pipelineModDao.findByNameIn(byNames)
        if (namesFound.size != names.size) {
            val missing = trimmedNames.minus(namesFound.map { it.name })
            throw DataRetrievalFailureException("The Pipeline Mods ${missing.joinToString(",")} do not exist.")
        }

        val idsFound = getByIds(byIds)
        return namesFound.plus(idsFound)
    }

    @Transactional(readOnly = true)
    override fun search(filter: PipelineModFilter): KPagedList<PipelineMod> {
        return pipelineModCustomDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: PipelineModFilter): PipelineMod {
        return pipelineModCustomDao.findOne(filter)
    }

    override fun create(spec: PipelineModSpec): PipelineMod {

        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()

        val mod = PipelineMod(
            id,
            spec.name,
            spec.description,
            spec.provider,
            spec.category,
            spec.type,
            spec.supportedMedia.map { it.name },
            spec.restricted,
            spec.ops,
            time, time, actor, actor, spec.standard
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
        val current = get(id)
        entityManager.detach(current)
        val updated = current.getUpdated(update)
        pipelineModDao.save(updated)
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
    @Transactional(propagation = Propagation.SUPPORTS)
    override fun updateStandardMods() {
        logger.info("Updating Standard Pipeline Mods")

        val mods = getStandardModules()
        val updated = mutableListOf<UUID>()

        withAuth(InternalThreadAuthentication(KnownKeys.PROJZERO)) {
            for (mod in mods) {
                val pmod = pipelineModDao.getByName(mod.name)
                try {
                    if (pmod == null) {
                        val m = create(mod)
                        updated.add(m.id)
                    } else {
                        val update = PipelineModUpdate(
                            pmod.name, pmod.description, pmod.provider,
                            pmod.category, pmod.type,
                            pmod.supportedMedia.map { SupportedMedia.valueOf(it) },
                            pmod.restricted, pmod.ops
                        )
                        update(pmod.id, update)
                        updated.add(pmod.id)
                    }
                } catch (ex: Exception) {
                    logger.warn("Failed to update standard pipeline mod, $pmod", ex)
                }
            }
        }
        // Remove mods we didn't update
        pipelineModDao.removeByIdNotIn(updated)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineModServiceImpl::class.java)
    }
}
