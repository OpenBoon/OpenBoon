package boonai.archivist.repository

import boonai.archivist.domain.Processor
import boonai.archivist.domain.ProcessorFilter
import boonai.archivist.domain.ProcessorSpec
import boonai.archivist.util.JdbcUtils
import boonai.archivist.util.JdbcUtils.getTsWordVector
import boonai.common.util.Json
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.UUID

interface ProcessorDao {
    fun batchCreate(specs: List<ProcessorSpec>): Int
    fun deleteAll()
    fun getAll(filter: ProcessorFilter): KPagedList<Processor>
    fun count(filter: ProcessorFilter): Long
    fun get(id: UUID): Processor
    fun get(name: String): Processor
    fun findOne(filter: ProcessorFilter): Processor
}

@Repository
class ProcessorDaoImpl : AbstractDao(), ProcessorDao {

    override fun batchCreate(specs: List<ProcessorSpec>): Int {

        val time = System.currentTimeMillis()
        val result = jdbc.batchUpdate(
            INSERT,
            object : BatchPreparedStatementSetter {

                @Throws(SQLException::class)
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val spec = specs[i]
                    val id = uuid3.generate(spec.className)
                    val types = if (spec.fileTypes == null) {
                        emptyArray()
                    } else {
                        spec.fileTypes.toTypedArray()
                    }

                    ps.setObject(1, id)
                    ps.setString(2, spec.className)
                    ps.setString(3, spec.file)
                    ps.setString(4, spec.type)
                    ps.setLong(5, time)
                    ps.setString(6, Json.serializeToString(spec.display, "[]"))
                    ps.setArray(7, ps.connection.createArrayOf("text", types))
                    ps.setObject(8, getTsWordVector(spec.className))
                }

                override fun getBatchSize(): Int {
                    return specs.size
                }
            }
        )

        return result.sum()
    }

    override fun deleteAll() {
        jdbc.update("TRUNCATE processor")
    }

    override fun get(id: UUID): Processor {
        return jdbc.queryForObject("$GET WHERE pk_processor=?", MAPPER, id)
    }

    override fun get(name: String): Processor {
        return jdbc.queryForObject("$GET WHERE str_name=?", MAPPER, name)
    }

    override fun getAll(filter: ProcessorFilter): KPagedList<Processor> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun findOne(filter: ProcessorFilter): Processor {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }

    override fun count(filter: ProcessorFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    companion object {

        private val INSERT = JdbcUtils.insert(
            "processor",
            "pk_processor",
            "str_name",
            "str_file",
            "str_type",
            "time_updated",
            "json_display::jsonb",
            "list_file_types",
            "fti_keywords@to_tsvector"
        )

        private const val GET = "SELECT * FROM processor"

        private const val COUNT = "SELECT COUNT(1) FROM processor"

        private val MAPPER = RowMapper { rs, _ ->
            Processor(
                rs.getObject("pk_processor") as UUID,
                rs.getString("str_name"),
                rs.getString("str_type"),
                rs.getString("str_file"),
                (rs.getArray("list_file_types").array as Array<String>).toList(),
                Json.Mapper.readValue(rs.getString("json_display"), Json.LIST_OF_GENERIC_MAP),
                rs.getLong("time_updated")
            )
        }
    }
}
