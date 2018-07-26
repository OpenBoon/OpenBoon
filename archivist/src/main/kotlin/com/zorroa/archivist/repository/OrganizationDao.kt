package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.common.domain.PagedList
import com.zorroa.common.domain.Pager
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface OrganizationDao : GenericNamedDao<Organization, OrganizationSpec> {
    fun getOnlyOne(): Organization
}

@Repository
class OrganizationDaoImpl : AbstractDao(), OrganizationDao {

    override fun getAll(): List<Organization> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun create(spec: OrganizationSpec): Organization {
        Preconditions.checkNotNull(spec.name,
                "The organization cannot be null")
        val id = uuid1.generate()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.name)
            ps
        })
        return get(id)
    }

    override fun get(id: UUID): Organization {
        return jdbc.queryForObject("$GET WHERE pk_organization=?", MAPPER, id)
    }

    override fun refresh(obj: Organization): Organization {
        return get(obj.id)
    }

    override fun getOnlyOne(): Organization {
        return  jdbc.queryForObject("$GET LIMIT 1", MAPPER)
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
        return jdbc.queryForObject("SELECT COUNT(1) FROM organization", Long::class.java)
    }

    override fun get(name: String): Organization {
        return jdbc.queryForObject("$GET WHERE str_name=?", MAPPER, name)
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM organization WHERE str_name=?", Integer::class.java, name) > 0
    }

    companion object {
        private val MAPPER = RowMapper<Organization> { rs, _ ->
            Organization(
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_name"))
        }

        private val GET = "SELECT pk_organization, str_name FROM organization"
        private val INSERT = JdbcUtils.insert("organization",
                "pk_organization", "str_name")
}

}
