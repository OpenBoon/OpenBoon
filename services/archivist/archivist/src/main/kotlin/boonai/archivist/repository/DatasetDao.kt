package boonai.archivist.repository

import boonai.archivist.domain.Dataset
import boonai.archivist.domain.DatasetFilter
import boonai.archivist.domain.DatasetType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DatasetDao : JpaRepository<Dataset, UUID> {

    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): Dataset?
    fun getOneByProjectIdAndName(projectId: UUID, name: String): Dataset?
    fun existsByProjectIdAndId(projectId: UUID, id: UUID): Boolean
}

interface DatasetJdbcDao {
    fun count(filter: DatasetFilter): Long
    fun findOne(filter: DatasetFilter): Dataset
    fun delete(dataset: Dataset): Boolean
    fun find(filter: DatasetFilter): KPagedList<Dataset>
    fun incrementModelCount(datasetPk: UUID?): Boolean
    fun decrementModelCount(datasetPk: UUID?): Boolean
}

@Repository
class DatasetJdbcDaoImpl : AbstractDao(), DatasetJdbcDao {

    override fun count(filter: DatasetFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: DatasetFilter): Dataset {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("DataSet not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun delete(model: Dataset): Boolean {
        return jdbc.update("DELETE FROM dataset where pk_dataset=?", model.id) == 1
    }

    override fun find(filter: DatasetFilter): KPagedList<Dataset> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun incrementModelCount(datasetPk: UUID?): Boolean {
        return datasetPk?.let { sumValueDatasetModelCount(it, 1) } ?: false
    }

    override fun decrementModelCount(datasetPk: UUID?): Boolean {
        return datasetPk?.let { sumValueDatasetModelCount(it, -1) } ?: false
    }

    private fun sumValueDatasetModelCount(datasetPk: UUID, value: Int): Boolean {
        return jdbc.update(
            "UPDATE dataset SET int_model_count = int_model_count + $value WHERE pk_dataset  = ?",
            datasetPk
        ) == 1
    }

    private val MAPPER = RowMapper { rs, _ ->
        Dataset(
            rs.getObject("pk_dataset") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getString("str_name"),
            DatasetType.values()[rs.getInt("int_type")],
            rs.getString("str_descr"),
            rs.getInt("int_model_count"),
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
