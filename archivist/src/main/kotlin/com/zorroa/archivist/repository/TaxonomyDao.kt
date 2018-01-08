package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.Taxonomy
import com.zorroa.archivist.domain.TaxonomySpec
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface TaxonomyDao : GenericDao<Taxonomy, TaxonomySpec> {

    operator fun get(folder: Folder): Taxonomy

    fun setActive(id: Taxonomy, value: Boolean): Boolean
}

@Repository
open class TaxonomyDaoImpl : AbstractDao(), TaxonomyDao {

    override fun create(spec: TaxonomySpec): Taxonomy {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_taxonomy"))
            ps.setInt(1, spec.folderId!!)
            ps
        }, keyHolder)
        return get(keyHolder.key.toInt())
    }

    override fun get(id: Int): Taxonomy {
        return jdbc.queryForObject<Taxonomy>(GET + "WHERE pk_taxonomy=?", MAPPER, id)
    }

    override fun get(folder: Folder): Taxonomy {
        return jdbc.queryForObject<Taxonomy>(GET + "WHERE pk_folder=?", MAPPER, folder.id)
    }

    override fun refresh(`object`: Taxonomy): Taxonomy {
        return get(`object`.taxonomyId)
    }

    override fun getAll(): List<Taxonomy> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getAll(paging: Pager): PagedList<Taxonomy>? {
        return null
    }

    override fun update(id: Int, spec: Taxonomy): Boolean {
        return false
    }

    override fun setActive(id: Taxonomy, value: Boolean): Boolean {
        return if (value) {
            jdbc.update("UPDATE taxonomy SET time_started=?, time_stopped=0, bool_active=? WHERE pk_taxonomy=? AND bool_active=?",
                    System.currentTimeMillis(), true, id.taxonomyId, false) == 1
        } else {
            jdbc.update("UPDATE taxonomy SET time_stopped=?, bool_active=? WHERE pk_taxonomy=? AND bool_active=?",
                    System.currentTimeMillis(), false, id.taxonomyId, true) == 1
        }
    }

    override fun delete(id: Int): Boolean {
        return jdbc.update("DELETE FROM taxonomy WHERE pk_taxonomy=?", id) == 1
    }

    override fun count(): Long {
        return 0
    }

    companion object {


        private val INSERT = JdbcUtils.insert("taxonomy",
                "pk_folder")

        private val GET = "SELECT " +
                "pk_taxonomy," +
                "pk_folder, " +
                "bool_active," +
                "time_started," +
                "time_stopped " +
                "FROM " +
                "taxonomy "

        private val MAPPER = RowMapper<Taxonomy> { rs, _ ->
            val tax = Taxonomy()
            tax.folderId = rs.getInt("pk_folder")
            tax.taxonomyId = rs.getInt("pk_taxonomy")
            tax.isActive = rs.getBoolean("bool_active")
            tax.timeStarted = rs.getLong("time_started")
            tax.timeStopped = rs.getLong("time_stopped")
            tax
        }
    }
}
