package boonai.archivist.service

import boonai.archivist.config.ArchivistConfiguration
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineModFilter
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineModUpdate
import boonai.archivist.domain.getStandardModules
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.PipelineModDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.InternalThreadAuthentication
import boonai.archivist.security.KnownKeys
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.security.withAuth
import boonai.archivist.util.isUUID
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface PipelineModService {
    fun create(spec: PipelineModSpec): PipelineMod
    fun get(id: UUID): PipelineMod
    fun getByName(name: String): PipelineMod
    fun getByNames(names: Collection<String>): List<PipelineMod>
    fun getByIds(names: Collection<UUID>): List<PipelineMod>
    fun search(filter: PipelineModFilter): KPagedList<PipelineMod>
    fun findOne(filter: PipelineModFilter): PipelineMod
    fun update(id: UUID, update: PipelineModUpdate): PipelineMod
    fun delete(id: UUID)
    fun findByName(name: String, standard: Boolean): PipelineMod?
    fun updateStandardMods()
}

@Service
@Transactional
class PipelineModServiceImpl(
    val pipelineModDao: PipelineModDao
) : PipelineModService {

    @Transactional(readOnly = true)
    override fun get(id: UUID): PipelineMod = pipelineModDao.get(id)

    @Transactional(readOnly = true)
    override fun getByName(name: String): PipelineMod {
        return pipelineModDao.getByName(name)
    }

    @Transactional(readOnly = true)
    override fun findByName(name: String, standard: Boolean): PipelineMod? {
        return pipelineModDao.findByName(name, standard)
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
        val forcedModules = names.filter { it.startsWith('+') }.map { it.trim('+') }.toSet()
        val trimmedNames = names.map { it.trim('+') }
        val byIds = trimmedNames.filter { it.isUUID() }.map { UUID.fromString(it) }
        val byNames = trimmedNames.filter { !it.isUUID() }

        val namesFound = pipelineModDao.findByNameIn(byNames)
        if (namesFound.size != names.size) {
            val missing = trimmedNames.minus(namesFound.map { it.name })
            throw DataRetrievalFailureException("The Pipeline Mods ${missing.joinToString(",")} do not exist.")
        }

        val idsFound = getByIds(byIds)
        val result = namesFound.plus(idsFound)
        result.forEach {
            if (it.name in forcedModules) {
                it.force = true
            }
        }

        return result
    }

    @Transactional(readOnly = true)
    override fun search(filter: PipelineModFilter): KPagedList<PipelineMod> {
        return pipelineModDao.getAll(filter)
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: PipelineModFilter): PipelineMod {
        return pipelineModDao.findOne(filter)
    }

    override fun create(spec: PipelineModSpec): PipelineMod {

        if (!spec.standard &&
            pipelineModDao.findByName(spec.name, true) != null
        ) {
            throw DataIntegrityViolationException("A module named '${spec.name}' already exists")
        }

        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()

        val projectId = if (spec.standard) {
            logger.info("Creating standard module: {}", spec.name)
            null
        } else {
            logger.info("Creating project module: {}", id)
            getProjectId()
        }

        val mod = PipelineMod(
            id,
            projectId,
            spec.name,
            spec.description,
            spec.provider,
            spec.category,
            spec.type,
            spec.supportedMedia.map { it.name },
            spec.ops,
            time, time, actor, actor
        )

        pipelineModDao.create(mod)
        logger.event(
            LogObject.PIPELINE_MODULE, LogAction.CREATE,
            mapOf(
                "pipelineModuleId" to id,
                "pipelineModuleName" to spec.name
            )
        )
        return mod
    }

    override fun update(id: UUID, update: PipelineModUpdate): PipelineMod {
        logger.event(LogObject.PIPELINE_MODULE, LogAction.UPDATE, mapOf("pipelineModId" to id))
        pipelineModDao.update(id, update)
        return get(id)
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
                val pmod = pipelineModDao.findByName(mod.name, true)
                try {
                    if (pmod == null) {
                        val m = create(mod)
                        updated.add(m.id)
                    } else {
                        val update = PipelineModUpdate(
                            pmod.name, mod.description, mod.provider,
                            mod.category, mod.type,
                            mod.supportedMedia,
                            mod.ops
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
        pipelineModDao.removeStandardByIdNotIn(updated)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineModServiceImpl::class.java)
    }
}
