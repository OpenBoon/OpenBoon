package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetFilter
import com.zorroa.archivist.domain.DataSetType
import com.zorroa.archivist.security.getZmlpActor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DataSetDao : JpaRepository<DataSet, UUID> {
    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): DataSet
    fun getOneByProjectIdAndName(projectId: UUID, name: String): DataSet

    fun existsByProjectIdAndId(projectId: UUID, dataSetId: UUID): Boolean
}

interface DataSetJdbcDao {

    /**
     * Find a [KPagedList] of [DataSet] that match the given [DataSetFilter]
     * The [Project] filter is applied automatically.
     */
    fun find(filter: DataSetFilter): KPagedList<DataSet>

    /**
     * Find one and only one [KPagedList] of [DataSet] that match
     * the given [DataSetFilter]. The [Project] filter is applied automatically.
     */
    fun findOne(filter: DataSetFilter): DataSet

    /**
     * Count the number of [DataSet] that match the given [DataSetFilter]
     * The [Project] filter is applied automatically.
     */
    fun count(filter: DataSetFilter): Long

    /**
     * Update [DataSet] modified state.
     */
    fun updateModified(dataSetID: UUID, modified: Boolean): Boolean
}

@Repository
class DataSetJdbcDaoImpl : AbstractDao(), DataSetJdbcDao {

    override fun count(filter: DataSetFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun updateModified(dataSetID: UUID, modified: Boolean): Boolean {
        return jdbc.update(SET_MODIFIED, System.currentTimeMillis(), getZmlpActor().toString(), modified, dataSetID) == 1
    }

    override fun findOne(filter: DataSetFilter): DataSet {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("DataSet not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun find(filter: DataSetFilter): KPagedList<DataSet> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private val MAPPER = RowMapper { rs, _ ->
        DataSet(
            rs.getObject("pk_data_set") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getString("str_name"),
            DataSetType.values()[rs.getInt("int_type")],
            rs.getLong("time_created"),
            rs.getLong("time_modified"),
            rs.getString("actor_created"),
            rs.getString("actor_modified"),
            rs.getBoolean("bool_modified")
        )
    }

    companion object {

        const val GET = "SELECT * FROM data_set"
        const val COUNT = "SELECT COUNT(1) FROM data_set"

        const val SET_MODIFIED = "UPDATE data_set SET time_modified=?, actor_modified=?, " +
            "bool_modified=? WHERE pk_data_set=?"
    }
}
