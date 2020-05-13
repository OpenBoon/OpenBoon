package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectSettings
import com.zorroa.archivist.util.JdbcUtils
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectDao : JpaRepository<Project, UUID> {
    fun getByName(name: String): Project

    @Query("update Project p set p.enabled = ?1 where p.id = ?2")
    @Modifying(clearAutomatically = true)
    fun updateStatus(enabled: Boolean, projectId: UUID)
}

interface ProjectCustomDao {
    fun findOne(filter: ProjectFilter): Project
    fun getAll(filter: ProjectFilter): KPagedList<Project>
    fun count(filter: ProjectFilter): Long

    fun getSettings(projectId: UUID): ProjectSettings
    fun updateSettings(projectId: UUID, settings: ProjectSettings): Boolean
    fun createSettings(projectId: UUID, settings: ProjectSettings)
}

@Repository
class ProjectCustomDaoImpl : ProjectCustomDao, AbstractDao() {

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun getSettings(projectId: UUID): ProjectSettings {
        return jdbc.queryForObject("SELECT * FROM project_settings WHERE pk_project=?",
            RowMapper { rs, _ ->
                ProjectSettings(
                    rs.getObject("pk_pipeline_default") as UUID,
                    rs.getObject("pk_index_route_default") as UUID
                )
            }, projectId)
    }

    override fun updateSettings(projectId: UUID, settings: ProjectSettings): Boolean {
        return jdbc.update(UPDATE_SETTINGS,
            settings.defaultPipelineId, settings.defaultIndexRouteId, projectId) == 1
    }

    override fun createSettings(projectId: UUID, settings: ProjectSettings) {
        jdbc.update(INSERT_SETTINGS, UUID.randomUUID(),
            projectId, settings.defaultPipelineId, settings.defaultIndexRouteId)
    }

    override fun count(filter: ProjectFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: ProjectFilter): Project {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Project not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getAll(filter: ProjectFilter): KPagedList<Project> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    companion object {
        const val GET = "SELECT * FROM project"
        const val COUNT = "SELECT COUNT(1) FROM project"
        const val UPDATE_SETTINGS = "UPDATE " +
            "project_settings SET pk_pipeline_default=?, pk_index_route_default=? WHERE pk_project=?"
        val INSERT_SETTINGS = JdbcUtils.insert("project_settings",
            "pk_project_settings",
            "pk_project",
            "pk_pipeline_default",
            "pk_index_route_default")

        private val MAPPER = RowMapper { rs, _ ->
            Project(
                rs.getObject("pk_project") as UUID,
                rs.getString("str_name"),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified"),
                rs.getBoolean("enabled")
            )
        }
    }
}
