package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.service.event
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProjectDao : JpaRepository<Project, UUID> {
    fun getByName(name: String): Project
}

interface ProjectFilterDao {
    fun findOne(filter: ProjectFilter): Project
    fun getAll(filter: ProjectFilter): KPagedList<Project>
    fun count(filter: ProjectFilter): Long
    fun deleteByUUID(projectUUID: UUID): Boolean
}

@Repository
class ProjectFilterDaoImpl : ProjectFilterDao, AbstractDao() {

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

    override fun deleteByUUID(projectUUID: UUID): Boolean {

        val result = listOf(
            "DELETE FROM job WHERE pk_project = ?",
            "DELETE FROM datasource WHERE pk_project = ?",
            "DELETE FROM index_route WHERE pk_project = ?",
            "DELETE FROM project WHERE pk_project = ?",
            "DELETE FROM auth.api_key WHERE project_id = ?"
        ).map { jdbc.update(it, projectUUID) }

        val lastResult = result.last() == 1;
        if (lastResult) {
            logger.event(LogObject.JOB, LogAction.DELETE)
            logger.event(LogObject.DATASOURCE, LogAction.DELETE)
            logger.event(LogObject.INDEX_ROUTE, LogAction.DELETE)
            logger.event(LogObject.PROJECT, LogAction.DELETE)
        }

        return lastResult
    }

    companion object {
        const val GET = "SELECT * FROM project"
        const val COUNT = "SELECT COUNT(1) FROM project"

        private val MAPPER = RowMapper { rs, _ ->
            Project(
                rs.getObject("pk_project") as UUID,
                rs.getString("str_name"),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified")
            )
        }
    }
}