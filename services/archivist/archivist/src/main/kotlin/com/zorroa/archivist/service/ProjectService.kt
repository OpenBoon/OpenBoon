package com.zorroa.archivist.service

import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileCategory
import com.zorroa.archivist.domain.FileGroup
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.domain.IndexRouteState
import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.ProjectDao
import com.zorroa.archivist.repository.ProjectFilterDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.KnownKeys
import com.zorroa.archivist.security.Perm
import com.zorroa.archivist.security.Role
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.storage.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
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
     * Delete project by unique Id.
     */
    fun delete(id: UUID)
}

@Service
@Transactional
class ProjectServiceImpl constructor(
    val projectDao: ProjectDao,
    val projectFilterDao: ProjectFilterDao,
    val authServerClient: AuthServerClient,
    val indexRoutingService: IndexRoutingService,
    val fileStorageService: FileStorageService,
    val jobService: JobService,
    val properties: ApplicationProperties,
    val txEvent: TransactionEventManager
) : ProjectService {

    @Lazy
    @Autowired
    lateinit var dataSourceService: DataSourceService


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
        createIndexRoute(project)
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

    private fun createIndexRoute(project: Project) {
        val mapping = properties.getString("archivist.es.default-mapping-type")
        val ver = properties.getInt("archivist.es.default-mapping-version")
        indexRoutingService.createIndexRoute(
            IndexRouteSpec(
                mapping, ver, projectId = project.id,
                state = IndexRouteState.CURRENT
            )
        )
    }

    /**
     * Create the list of standard project keys.
     */
    private fun createStandardApiKeys(project: Project) {
        logger.info("Creating standard API Keys for project ${project.name}")
        authServerClient.createApiKey(
            project, KnownKeys.JOB_RUNNER, listOf(
                Role.JOBRUNNER,
                Perm.ASSETS_WRITE,
                Perm.ASSETS_READ,
                Perm.STORAGE_CREATE
            )
        )
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

    override fun getCredentialsKey(): String {
        val loc = FileStorageLocator(FileGroup.INTERNAL, "project", FileCategory.KEYS, "project.key")
        return String(fileStorageService.fetch(loc))
    }

    @Transactional
    override fun delete(id: UUID) {
        //Get Project
        val project: Project = get(id)

        //Job Delete
        jobService.deleteAllProjectJobs(project)
        logger.event(LogObject.JOB, LogAction.DELETE)

        //DataSource Delete
        dataSourceService.deleteProjectDataSource(project)
        logger.event(LogObject.DATASOURCE, LogAction.DELETE)

        //Index Route Delete
        indexRoutingService.deleteIndexByProject(project)
        logger.event(LogObject.INDEX_ROUTE, LogAction.DELETE)

        //Project Delete
        projectDao.delete(project)
        logger.event(LogObject.PROJECT, LogAction.DELETE)

        //Delete Auth Api Key
        val apiKey = authServerClient.getApiKey(id)
        authServerClient.deleteApiKey(apiKey.keyId)

    }

    override fun get(id: UUID): Project {
        return projectDao.getOne(id)
    }

    override fun get(name: String): Project {
        return projectDao.getByName(name)
    }

    override fun getAll(filter: ProjectFilter): KPagedList<Project> {
        return projectFilterDao.getAll(filter)
    }

    override fun findOne(filter: ProjectFilter): Project {
        return projectFilterDao.findOne(filter)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectServiceImpl::class.java)
    }
}