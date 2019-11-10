package com.zorroa.archivist.service

import com.zorroa.archivist.clients.AuthServerClient
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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
}

@Service
@Transactional
class ProjectServiceImpl constructor(
    val projectDao: ProjectDao,
    val projectFilterDao: ProjectFilterDao,
    val authServerClient: AuthServerClient
) : ProjectService {

    override fun create(spec: ProjectSpec): Project {
        val time = System.currentTimeMillis()
        val project = projectDao.saveAndFlush(
            Project(
                spec.projectId ?: UUIDGen.uuid1.generate(),
                spec.name,
                time,
                time
            )
        )

        createStandardKeys(project)

        logger.event(
            LogObject.PROJECT, LogAction.CREATE,
            mapOf(
                "newProjectId" to project.id,
                "newProjectName" to project.name
            )
        )
        return project
    }

    /**
     * Create the list of standard project keys.
     */
    private fun createStandardKeys(project: Project) {
        authServerClient.createApiKey(
            project, KnownKeys.JOB_RUNNER, listOf(
                Role.JOBRUNNER,
                Perm.ASSETS_IMPORT,
                Perm.ASSETS_READ,
                Perm.STORAGE_CREATE
            )
        )
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