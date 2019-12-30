package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
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