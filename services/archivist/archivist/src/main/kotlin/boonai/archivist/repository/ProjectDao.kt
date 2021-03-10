package boonai.archivist.repository

import boonai.archivist.domain.IndexRoute
import boonai.archivist.domain.Pipeline
import boonai.archivist.domain.Project
import boonai.archivist.domain.ProjectFilter
import boonai.archivist.domain.ProjectTier
import boonai.archivist.security.getZmlpActor
import boonai.archivist.util.JdbcUtils
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectDao : JpaRepository<Project, UUID> {
    fun getByName(name: String): Project
}

interface ProjectCustomDao {
    fun findOne(filter: ProjectFilter): Project
    fun getAll(filter: ProjectFilter): KPagedList<Project>
    fun count(filter: ProjectFilter): Long

    fun isEnabled(projectId: UUID): Boolean
    fun setEnabled(projectId: UUID, value: Boolean): Boolean

    fun updateTier(projectId: UUID, value: ProjectTier): Boolean
    fun updateName(projectId: UUID, value: String): Boolean
    fun updateDefaultPipeline(projectId: UUID, pipeline: Pipeline): Boolean
    fun updateIndexRoute(projectId: UUID, indexRoute: IndexRoute): Boolean
}

@Repository
class ProjectCustomDaoImpl : ProjectCustomDao, AbstractDao() {

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

    override fun isEnabled(projectId: UUID): Boolean {
        return jdbc.queryForObject(
            "SELECT COUNT(1) FROM project WHERE pk_project=? AND enabled='t'",
            Int::class.java, projectId
        ) == 1
    }

    override fun setEnabled(projectId: UUID, value: Boolean): Boolean {
        return jdbc.update(
            SET_ENABLED, value, System.currentTimeMillis(),
            getZmlpActor().toString(), projectId, value
        ) == 1
    }

    override fun updateTier(projectId: UUID, value: ProjectTier): Boolean {
        return jdbc.update(
            SET_TIER, value.ordinal, System.currentTimeMillis(),
            getZmlpActor().toString(), projectId
        ) == 1
    }

    override fun updateName(projectId: UUID, value: String): Boolean {
        return jdbc.update(
            SET_NAME, value, System.currentTimeMillis(),
            getZmlpActor().toString(), projectId
        ) == 1
    }

    override fun updateDefaultPipeline(projectId: UUID, pipeline: Pipeline): Boolean {
        return jdbc.update(
            SET_PIPELINE, pipeline.id,
            System.currentTimeMillis(), getZmlpActor().toString(), projectId
        ) == 1
    }

    override fun updateIndexRoute(projectId: UUID, indexRoute: IndexRoute): Boolean {
        return jdbc.update(
            SET_INDEX, indexRoute.id, System.currentTimeMillis(), getZmlpActor().toString(), projectId
        ) == 1
    }

    companion object {
        const val GET = "SELECT * FROM project"
        const val COUNT = "SELECT COUNT(1) FROM project"
        const val UPDATE_SETTINGS = "UPDATE project_settings " +
            "SET pk_pipeline_default=?, pk_index_route_default=? WHERE pk_project=?"
        const val SET_ENABLED = "UPDATE project " +
            "SET enabled=?, time_modified=?, actor_modified=? WHERE pk_project=? AND enabled != ?"
        const val SET_TIER = "UPDATE project " +
            "SET int_tier=?, time_modified=?, actor_modified=? WHERE pk_project=?"
        const val SET_NAME = "UPDATE project " +
            "SET str_name=?, time_modified=?, actor_modified=? WHERE pk_project=?"
        const val SET_PIPELINE = "UPDATE project " +
            "SET pk_pipeline_default=?, time_modified=?, actor_modified=? WHERE pk_project=?"
        const val SET_INDEX = "UPDATE project " +
            "SET pk_index_route=?, time_modified=?, actor_modified=? WHERE pk_project=?"

        val INSERT_SETTINGS = JdbcUtils.insert(
            "project_settings",
            "pk_project_settings",
            "pk_project",
            "pk_pipeline_default",
            "pk_index_route_default"
        )

        private val MAPPER = RowMapper { rs, _ ->
            Project(
                rs.getObject("pk_project") as UUID,
                rs.getString("str_name"),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified"),
                rs.getBoolean("enabled"),
                ProjectTier.values()[(rs.getInt("int_tier"))]
            )
        }
    }
}
