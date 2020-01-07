package com.zorroa.archivist.service

import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileCategory
import com.zorroa.archivist.domain.FileGroup
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.IndexRoute
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.IndexRouteState
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSettings
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.ProjectCustomDao
import com.zorroa.archivist.repository.ProjectDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.repository.throwWhenNotFound
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.security.withAuth
import com.zorroa.archivist.storage.FileStorageService
import com.zorroa.auth.client.AuthServerClient
import com.zorroa.auth.client.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Base64
import java.util.UUID

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
    fun getCredentialsKey(): String

    /**
     * Get the project settings
     */
    fun getSettings(projectId: UUID): ProjectSettings

    /**
     * Get the project settings blob.
     */
    fun updateSettings(projectId: UUID, settings: ProjectSettings): Boolean
}

@Service
@Transactional
class ProjectServiceImpl constructor(
    val projectDao: ProjectDao,
    val projectCustomDao: ProjectCustomDao,
    val authServerClient: AuthServerClient,
    val fileStorageService: FileStorageService,
    val properties: ApplicationProperties,
    val txEvent: TransactionEventManager
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
                spec.projectId ?: UUIDGen.uuid1.generate(),
                spec.name,
                time,
                time,
                actor.name,
                actor.name
            )
        )
        withAuth(InternalThreadAuthentication(project.id, setOf())) {

            val route = createIndexRoute(project)
            val pipeline = createDefaultPipeline(project)
            projectCustomDao.createSettings(
                project.id, ProjectSettings(
                    pipeline.id,
                    route.id
                )
            )
        }
        txEvent.afterCommit(sync = true) {
            createProjectCryptoKey(project)
            createStandardApiKeys(project)
        }

        logger.event(
            LogObject.PROJECT, LogAction.CREATE,
            mapOf(
                "newProjectId" to project.id,
                "newProjectName" to project.name
            )
        )
        return project
    }

    private fun createIndexRoute(project: Project): IndexRoute {
        val mapping = properties.getString("archivist.es.default-mapping-type")
        val ver = properties.getInt("archivist.es.default-mapping-version")
        return indexRoutingService.createIndexRoute(
            IndexRouteSpec(
                mapping, ver, projectId = project.id,
                state = IndexRouteState.READY
            )
        )
    }

    override fun getCredentialsKey(): String {
        val loc = FileStorageLocator(FileGroup.INTERNAL, "project", FileCategory.KEYS, "project.key")
        return String(fileStorageService.fetch(loc))
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Project = projectDao.getOne(id)

    @Transactional(readOnly = true)
    override fun get(name: String): Project = projectDao.getByName(name)

    @Transactional(readOnly = true)
    override fun getAll(filter: ProjectFilter): KPagedList<Project> = projectCustomDao.getAll(filter)

    @Transactional(readOnly = true)
    override fun findOne(filter: ProjectFilter): Project = projectCustomDao.findOne(filter)

    @Transactional(readOnly = true)
    override fun getSettings(projectId: UUID): ProjectSettings = projectCustomDao.getSettings(get(projectId).id)

    override fun updateSettings(projectId: UUID, settings: ProjectSettings): Boolean {
        val project = get(projectId)

        if (!hasPermission(listOf(Permission.ProjectManage))) {
            throw DataRetrievalFailureException("Unable to find project: $projectId")
        }

        /**
         * Validate these IDs exist and are within the same project.
         */
        throwWhenNotFound("Unable to find Pipeline ID ${settings.defaultPipelineId}") {
            pipelineService.get(settings.defaultPipelineId)
        }
        throwWhenNotFound("Unable to find Index ID ${settings.defaultIndexRouteId}") {
            indexRoutingService.getIndexRoute(settings.defaultIndexRouteId)
        }
        return projectCustomDao.updateSettings(project.id, settings)
    }

    /**
     * Create the list of standard project keys.
     */
    private fun createStandardApiKeys(project: Project) {
        logger.info("Creating standard API Keys for project ${project.name}")
        authServerClient.createApiKey(
            project.id, KnownKeys.JOB_RUNNER, setOf(
                Permission.AssetsImport,
                Permission.AssetsRead,
                Permission.SystemProjectDecrypt
            )
        )
    }

    private fun createDefaultPipeline(project: Project): Pipeline {
        val spec = PipelineSpec("default", PipelineMode.MODULAR, projectId = project.id)
        return pipelineService.create(spec)
    }

    private fun createProjectCryptoKey(project: Project) {
        val projectKeyLocation = FileStorageLocator(
            FileGroup.INTERNAL, "project", FileCategory.KEYS, "project.key",
            projectId = project.id
        )

        val key = Base64.getUrlEncoder().encodeToString(
            KeyGenerators.secureRandom(32).generateKey()
        ).trim('=')

        val spec = FileStorageSpec(projectKeyLocation, mapOf(), key.toByteArray())
        fileStorageService.store(spec)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
    }
}