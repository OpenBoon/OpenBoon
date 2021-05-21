package boonai.archivist.repository

import boonai.archivist.domain.DataSet
import boonai.archivist.domain.DataSetFilter
import boonai.archivist.domain.DataSetType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DataSetDao : JpaRepository<DataSet, UUID> {

    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): DataSet?
    fun getOneByProjectIdAndName(projectId: UUID, name: String): DataSet?
    fun existsByProjectIdAndId(projectId: UUID, id: UUID): Boolean
}

interface DataSetJdbcDao {
    fun count(filter: DataSetFilter): Long
    fun findOne(filter: DataSetFilter): DataSet
    fun delete(dataSet: DataSet): Boolean
    fun find(filter: DataSetFilter): KPagedList<DataSet>
}

@Repository
class DataSetJdbcDaoImpl : AbstractDao(), DataSetJdbcDao {

    override fun count(filter: DataSetFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: DataSetFilter): DataSet {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Model not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun delete(model: DataSet): Boolean {
        return jdbc.update("DELETE FROM dataset where pk_dataset=?", model.id) == 1
    }

    override fun find(filter: DataSetFilter): KPagedList<DataSet> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private val MAPPER = RowMapper { rs, _ ->
        DataSet(
            rs.getObject("pk_dataset") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getString("str_name"),
            DataSetType.values()[rs.getInt("int_type")],
            rs.getString("str_desc"),
            rs.getLong("time_created"),
            rs.getLong("time_modified"),
            rs.getString("actor_created"),
            rs.getString("actor_modified")
        )
    }

    companion object {

        const val GET = "SELECT * FROM dataset"
        const val COUNT = "SELECT COUNT(1) FROM dataset"
    }
}
