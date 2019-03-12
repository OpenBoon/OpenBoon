package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.util.*

interface TaxonomyDao : GenericDao<Taxonomy, TaxonomySpec> {

    operator fun get(folder: Folder): Taxonomy
}

@Repository
class TaxonomyDaoImpl : AbstractDao(), TaxonomyDao {

    override fun create(spec: TaxonomySpec): Taxonomy {
        val keyHolder = GeneratedKeyHolder()
        val id = uuid1.generate()
        val user = getUser()
        val time = System.currentTimeMillis()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(
                    INSERT, arrayOf("pk_taxonomy"))
            ps.setObject(1, id)
            ps.setObject(2, spec.folderId)
            ps.setObject(3, user.organizationId)
            ps.setObject(4, user.id)
            ps.setLong(5, time)
            ps
        }, keyHolder)

        logger.event(LogObject.TAXONOMY, LogAction.CREATE, mapOf("taxonomyId" to id))
        return get(id)
    }

    override fun get(id: UUID): Taxonomy {
        return jdbc.queryForObject<Taxonomy>(GET + "WHERE pk_organization=? AND pk_taxonomy=?",
                MAPPER, getOrgId(), id)
    }

    override fun get(folder: Folder): Taxonomy {
        return jdbc.queryForObject<Taxonomy>(GET + "WHERE pk_organization=? AND pk_folder=?",
                MAPPER, getOrgId(), folder.id)
    }

    override fun refresh(obj: Taxonomy): Taxonomy {
        return get(obj.taxonomyId)
    }

    override fun getAll(): List<Taxonomy> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getAll(paging: Pager): PagedList<Taxonomy> {
        return PagedList()
    }

    override fun update(id: UUID, spec: Taxonomy): Boolean {
        return false
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM taxonomy WHERE pk_organization=? AND pk_taxonomy=?", getOrgId(), id) == 1
    }

    override fun count(): Long {
        return 0
    }

    companion object {


        private val INSERT = JdbcUtils.insert("taxonomy",
                "pk_taxonomy",
                "pk_folder",
                "pk_organization",
                "pk_user_created",
                "time_created")

        private val GET = "SELECT " +
                "pk_taxonomy," +
                "pk_folder, " +
                "pk_organization, " +
                "pk_user_created," +
                "time_created "+
                "FROM " +
                "taxonomy "

        private val MAPPER = RowMapper { rs, _ ->
            Taxonomy(
                    rs.getObject("pk_taxonomy") as UUID,
                    rs.getObject("pk_folder") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getObject("pk_user_created") as UUID,
                    rs.getLong("time_created"))
        }
    }
}
