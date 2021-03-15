package boonai.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import boonai.archivist.config.ApplicationProperties
import boonai.archivist.domain.Project
import boonai.archivist.domain.ProjectSpec
import boonai.archivist.domain.ProjectFilter
import boonai.archivist.domain.ProjectQuotaCounters
import boonai.archivist.domain.ProjectQuotas
import boonai.archivist.domain.ProjectQuotasTimeSeriesEntry
import boonai.archivist.domain.ProjectNameUpdate
import boonai.archivist.domain.ProjectTier
import boonai.archivist.domain.ProjectSize
import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.IndexRouteSimpleSpec
import boonai.archivist.domain.Pipeline
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.IndexRouteFilter
import boonai.archivist.domain.ArchivistException
import boonai.archivist.domain.PipelineMode
import boonai.archivist.repository.KPagedList
import boonai.archivist.repository.ProjectDao
import boonai.archivist.repository.ProjectCustomDao
import boonai.archivist.repository.ProjectDeleteDao
import boonai.archivist.repository.ProjectQuotasDao
import boonai.archivist.repository.UUIDGen
import boonai.archivist.security.InternalThreadAuthentication
import boonai.archivist.security.KnownKeys
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.security.withAuth
import boonai.archivist.storage.ProjectStorageService
import boonai.common.apikey.AuthServerClient
import boonai.common.apikey.Permission
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.service.storage.SystemStorageService
import boonai.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Base64
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

interface ProjectService {
    /**
     * Create a [Project] with the the given [ProjectSpec]
     */
    fun create(spec: ProjectSpec): Project

    /**
     * Get a [Project] by unique Id.
     */
    fun get(id: UUID): Project

    /**
     * Get a [Project] by name.
     */
    fun get(name: String): Project

    /**
     * Return a [KPagedList] of all [Project]
     */
    fun getAll(filter: ProjectFilter): KPagedList<Project>

    /**
     * Find a single project with supplied [ProjectFilter]
     */
    fun findOne(filter: ProjectFilter): Project

    /**
     * Return the project's credentials key.  Use care if/when rotating this
     * key, any stored data would first have to be decrypted with the
     * old key.
     */
    fun getCryptoKey(): String

    /**
     * Get the the given projects quotas.
     */
    fun getQuotas(projectId: UUID): ProjectQuotas

    /**
     * Increment project quotas.
     */
    fun incrementQuotaCounters(counters: ProjectQuotaCounters)

    /**
     * Get project quota time series info.
     */
    fun getQuotasTimeSeries(projectId: UUID, start: Date, end: Date): List<ProjectQuotasTimeSeriesEntry>

    /**
     * Enable or disable the project.
     */
    fun setEnabled(projectId: UUID, value: Boolean)

    /**
     * Return true if the project ID is enabled, false if not.
     */
    fun isEnabled(id: UUID): Boolean

    /**
     * Update Project Tier
     */
    fun setTier(projectId: UUID, value: ProjectTier)

    /**
     * Rename Project
     */
    fun rename(projectId: UUID, newName: ProjectNameUpdate)

    /**
     * Set the active index for the given project.
     */
    fun setIndexRoute(project: Project, route: IndexRoute): Boolean

    /**
     * Delete System Storage files of a Project
     */
    fun deleteProjectSystemStorage(project: Project)

    /**
     * Delete Project related storage
     */
    fun deleteProjectStorage(project: Project)

    /**
     * Delete Project
     */
    fun delete(project: Project)
}

@Service
@Transactional
class ProjectServiceImpl constructor(
    val projectDao: ProjectDao,
    val projectCustomDao: ProjectCustomDao,
    val projectStatsDao: ProjectQuotasDao,
    val authServerClient: AuthServerClient,
    val systemStorageService: SystemStorageService,
    var projectStorageService: ProjectStorageService,
    val properties: ApplicationProperties,
    val txEvent: TransactionEventManager,
    val projectDeleteDao: ProjectDeleteDao
) : ProjectService {

    @Autowired
    lateinit var indexRoutingService: IndexRoutingService

    @Autowired
    lateinit var pipelineService: PipelineService

    override fun create(spec: ProjectSpec): Project {
        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val project = projectDao.saveAndFlush(
            Project(
                spec.id ?: UUIDGen.uuid1.generate(),
                spec.name,
                time,
                time,
                actor.toString(),
                actor.toString(),
                true,
                spec.tier
            )
        )

        withAuth(InternalThreadAuthentication(project.id, setOf())) {
            val route = createIndexRoute(project, spec.size)
            projectCustomDao.updateIndexRoute(project.id, route)

            val pipeline = createDefaultPipeline(project)
            projectCustomDao.updateDefaultPipeline(project.id, pipeline)

            projectStatsDao.createQuotasEntry(project.id)
            projectStatsDao.createIngestTimeSeriesEntries(project.id)
        }

        createCryptoKey(project)
        createStandardApiKeys(project)

        enabledCache.invalidate(project.id)

        logger.event(
            LogObject.PROJECT, LogAction.CREATE,
            mapOf(
                "newProjectId" to project.id,
                "newProjectName" to project.name
            )
        )
        return project
    }

    private fun createIndexRoute(project: Project, size: ProjectSize): IndexRoute {
        return indexRoutingService.createIndexRoute(
            IndexRouteSimpleSpec(
                size,
                project.id
            )
        )
    }

    override fun getCryptoKey(): String {
        val pid = getProjectId()
        val keys = systemStorageService.fetchObject(
            "projects/$pid/keys.json", Json.LIST_OF_STRING
        )
        // If this ever changes, things will break.
        val mod1 = (pid.leastSignificantBits % keys.size).toInt()
        val mod2 = (pid.mostSignificantBits % keys.size).toInt()
        return "${keys[mod1]}_${keys[mod2].reversed()}_${keys.last()}}"
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Project {
        return projectDao.getOne(id)
    }

    @Transactional(readOnly = true)
    override fun get(name: String): Project = projectDao.getByName(name)

    @Transactional(readOnly = true)
    override fun getAll(filter: ProjectFilter): KPagedList<Project> = projectCustomDao.getAll(filter)

    @Transactional(readOnly = true)
    override fun findOne(filter: ProjectFilter): Project = projectCustomDao.findOne(filter)

    @Transactional(readOnly = true)
    override fun getQuotas(projectId: UUID): ProjectQuotas = projectStatsDao.getQuotas(get(projectId).id)

    @Transactional(readOnly = true)
    override fun getQuotasTimeSeries(projectId: UUID, start: Date, end: Date): List<ProjectQuotasTimeSeriesEntry> =
        projectStatsDao.getTimeSeriesCounters(projectId, start, end)

    override fun incrementQuotaCounters(counters: ProjectQuotaCounters) {
        projectStatsDao.incrementQuotas(counters)
        projectStatsDao.incrementTimeSeriesCounters(Date(), counters)
    }

    /**
     * Create the list of standard project keys.
     */
    private fun createStandardApiKeys(project: Project) {
        logger.info("Creating standard API Keys for project ${project.name}")
        authServerClient.createApiKey(
            project.id, KnownKeys.JOB_RUNNER,
            setOf(
                Permission.AssetsImport,
                Permission.AssetsRead,
                Permission.SystemProjectDecrypt,
                Permission.ProjectFilesRead,
                Permission.ProjectFilesWrite
            ),
            true
        )
    }

    private fun createDefaultPipeline(project: Project): Pipeline {
        val spec = PipelineSpec("default", PipelineMode.MODULAR, projectId = project.id)
        return pipelineService.create(spec)
    }

    /**
     * Create a set of crypto keys for the project.  These are used
     * to encrypt data stored in the database.
     */
    private fun createCryptoKey(project: Project) {
        val result = (1..16).map {
            Base64.getUrlEncoder()
                .encodeToString(KeyGenerators.secureRandom(24).generateKey())
                .trim('=')
        }
        systemStorageService.storeObject(
            "projects/${project.id}/keys.json", result.toList()
        )
    }

    override fun deleteProjectSystemStorage(project: Project) {
        systemStorageService.recursiveDelete(
            "projects/${project.id}"
        )
        logger.info("Deleting System Storage of Project: ${project.name}")
    }

    override fun setEnabled(projectId: UUID, value: Boolean) {
        val project = projectDao.findById(projectId).orElseThrow {
            EmptyResultDataAccessException("Project not found", 1)
        }
        projectCustomDao.setEnabled(project.id, value)
        authServerClient.updateApiKeyEnabledByProject(project.id, value)
        enabledCache.invalidate(project.id)

        if (value)
            enableProject(project)
        else
            disableProject(project)
    }

    private fun enableProject(project: Project) {

        val indexRoute =
            if (project.indexRouteId != null)
                indexRoutingService.getIndexRoute(project.indexRouteId)
            else
                indexRoutingService.findOne(IndexRouteFilter(projectIds = listOf(project.id)))

        indexRoute?.let {
            indexRoutingService.openIndex(it)

            logger.event(
                LogObject.INDEX_ROUTE, LogAction.DISABLE,
                mapOf(
                    "indexRouteId" to indexRoute.id,
                    "cluster" to indexRoute.clusterId
                )
            )
        }

        logger.event(
            LogObject.PROJECT, LogAction.ENABLE,
            mapOf(
                "projectId" to project.id,
                "projectName" to project.name
            )
        )
    }

    private fun disableProject(project: Project) {
        val idxs = indexRoutingService.getAll(
            IndexRouteFilter(
                projectIds = listOf(project.id)
            )
        )

        idxs.forEach {
            indexRoutingService.closeIndex(it)

            logger.event(
                LogObject.INDEX_ROUTE, LogAction.DISABLE,
                mapOf(
                    "indexRoute" to it.id,
                    "cluster" to it.clusterId
                )
            )
        }

        logger.event(
            LogObject.PROJECT, LogAction.DISABLE,
            mapOf(
                "projectId" to project.id,
                "projectName" to project.name
            )
        )
    }

    @Transactional(readOnly = true)
    override fun isEnabled(id: UUID): Boolean {
        try {
            return enabledCache.get(id)
        } catch (e: Exception) {
            logger.warn("Unable to check for project enabled status.", e)
        }

        return false
    }

    override fun setTier(projectId: UUID, value: ProjectTier) {
        val project = projectDao.findById(projectId).orElseThrow {
            EmptyResultDataAccessException("Project not found", 1)
        }

        if (!projectCustomDao.updateTier(projectId, value)) {
            throw ArchivistException("Project Tier update failed")
        }

        logger.event(
            LogObject.PROJECT, LogAction.UPDATE,
            mapOf(
                "projectId" to project.id,
                "projectName" to project.name,
                "projectTier" to value
            )
        )
    }

    override fun rename(projectId: UUID, newName: ProjectNameUpdate) {
        val project = projectDao.findById(projectId).orElseThrow {
            EmptyResultDataAccessException("Project not found", 1)
        }

        if (!projectCustomDao.updateName(projectId, newName.name)) {
            throw ArchivistException("Project Rename Failed")
        }
        enabledCache.invalidate(projectId)

        logger.event(
            LogObject.PROJECT, LogAction.UPDATE,
            mapOf(
                "projectId" to project.id,
                "projectName" to project.name,
                "projectNewName" to newName.name
            )
        )
    }

    override fun setIndexRoute(project: Project, route: IndexRoute): Boolean {
        return indexRoutingService.setIndexRoute(project, route)
    }

    override fun deleteProjectStorage(project: Project) {
        projectStorageService.recursiveDelete("projects/${project.id}")
        logger.warn("Deleted Project ${project.id} storage files")
    }

    @Transactional
    override fun delete(project: Project) {

        projectDeleteDao.deleteProjectRelatedObjects(project.id)

        // Delete API Key
        // Delete System Storage
        // Delete Project Storage
        // Delete Project Indexes
        // Delete Project from Database

        authServerClient.deleteProjectApiKeys(projectId = project.id)
    }

    // This gets called alot so hold onto the values for a while.
    // Might have to go into redis.
    private val enabledCache = CacheBuilder.newBuilder()
        .initialCapacity(32)
        .concurrencyLevel(2)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(object : CacheLoader<UUID, Boolean>() {
            @Throws(Exception::class)
            override fun load(projectId: UUID): Boolean {
                return projectCustomDao.isEnabled(projectId)
            }
        })

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
    }
}
