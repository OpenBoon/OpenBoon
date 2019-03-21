package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.archivist.service.event
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface OrganizationDao : GenericNamedDao<Organization, OrganizationSpec> {
    fun findOne(filter: OrganizationFilter): Organization
    fun getAll(filter: OrganizationFilter): KPagedList<Organization>
    fun count(filter: OrganizationFilter): Long
    fun update(org: Organization, spec: OrganizationUpdateSpec) : Boolean
}

@Repository
class OrganizationDaoImpl : AbstractDao(), OrganizationDao {

    override fun getAll(): List<Organization> {
        return jdbc.query(GET, MAPPER)
    }

    override fun create(spec: OrganizationSpec): Organization {
        Preconditions.checkNotNull(spec.name,
                "The organization cannot be null")
        val id = uuid1.generate()
        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.name)
            ps
        }
        logger.event(LogObject.ORGANIZATION, LogAction.CREATE,
                mapOf("newOrgId" to id, "orgName" to spec.name))
        return get(id)
    }

    override fun update(org: Organization, spec: OrganizationUpdateSpec) : Boolean {
        return jdbc.update("UPDATE organization SET str_name=? WHERE pk_organization=?",
                spec.name, org.id) == 1
    }

    override fun get(id: UUID): Organization {
        try {
            return jdbc.queryForObject("$GET WHERE pk_organization=?", MAPPER, id)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("The organization ID '$id' does not exist", 1)
        }
    }

    override fun refresh(obj: Organization): Organization {
        return get(obj.id)
    }

    override fun findOne(filter: OrganizationFilter): Organization {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Organization not found") {
            KPagedList(1L, KPage(0, 1), jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getAll(filter: OrganizationFilter): KPagedList<Organization> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(filter: OrganizationFilter): Long {
        return jdbc.queryForObject(filter.getQuery(COUNT, forCount = true),
                Long::class.java, *filter.getValues(forCount = true))
    }

    override fun getAll(paging: Pager): PagedList<Organization> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(id: UUID, spec: Organization): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delete(id: UUID): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(): Long {
        return jdbc.queryForObject(COUNT, Long::class.java)
    }

    override fun get(name: String): Organization {
        try {
            return jdbc.queryForObject("$GET WHERE str_name=?", MAPPER, name)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("The organization name '$name' does not exist", 1)
        }
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM organization WHERE str_name=?", Integer::class.java, name) > 0
    }

    companion object {
        private val MAPPER = RowMapper { rs, _ ->
            Organization(
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"))
        }

        private const val GET = "SELECT pk_organization, str_name FROM organization"
        private const val COUNT = "SELECT COUNT(1) FROM organization"
        private val INSERT = JdbcUtils.insert("organization",
                "pk_organization", "str_name")
}

}
