package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Migration
import com.zorroa.archivist.domain.MigrationType
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*


/**
 * Created by chambers on 2/2/16.
 */
interface MigrationDao {

    fun getAll(): List<Migration>

    fun getAll(type: MigrationType): List<Migration>

    /**
     * Get the version of the migration to the given version.  If the version
     * changes, return true otherwise return false.
     *
     * @param m
     * @param version
     * @return
     */
    fun setVersion(m: Migration, version: Int): Boolean

    fun setVersion(m: Migration, version: Int, patch: Int): Boolean

    fun setPatch(m: Migration, patch: Int): Boolean
}

@Repository
open class MigrationDaoImpl : AbstractDao(), MigrationDao {

    override fun getAll(): List<Migration> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getAll(type: MigrationType): List<Migration> {
        return jdbc.query<Migration>(GET + " WHERE int_type=?", MAPPER, type.ordinal)
    }

    override fun setVersion(m: Migration, version: Int): Boolean {
        return jdbc.update("UPDATE migration SET int_version=? WHERE pk_migration=? AND int_version!=?",
                version, m.id, version) == 1
    }

    override fun setVersion(m: Migration, version: Int, patch: Int): Boolean {
        return jdbc.update("UPDATE migration SET int_version=?, int_patch=? WHERE pk_migration=?",
                version, patch, m.id) == 1
    }

    override fun setPatch(m: Migration, patch: Int): Boolean {
        return jdbc.update("UPDATE migration SET int_patch=? WHERE pk_migration=? AND int_patch!=?",
                patch, m.id, patch) == 1
    }

    companion object {

        private val MAPPER = RowMapper<Migration> { rs, _ ->
            val m = Migration()
            m.id = rs.getObject("pk_migration") as UUID
            m.name = rs.getString("str_name")
            m.type = MigrationType.values()[rs.getInt("int_type")]
            m.path = rs.getString("str_path")
            m.version = rs.getInt("int_version")
            m.patch = rs.getInt("int_patch")
            m
        }

        private val GET = "SELECT " +
                "pk_migration," +
                "str_name," +
                "int_type," +
                "str_path," +
                "int_version, " +
                "int_patch " +
                "FROM " +
                "migration "
    }
}
