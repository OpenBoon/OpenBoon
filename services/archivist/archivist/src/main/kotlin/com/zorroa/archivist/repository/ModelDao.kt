package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ModelDao : JpaRepository<Model, UUID> {

    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): Model
}

interface ModelJdbcDao {

    /**
     * Find a [KPagedList] of [Model] that match the given [ModelFilter]
     * The [Project] filter is applied automatically.
     */
    fun find(filter: ModelFilter): KPagedList<Model>

    /**
     * Find one and only one [KPagedList] of [Model] that match
     * the given [ModelFilter]. The [Project] filter is applied automatically.
     */
    fun findOne(filter: ModelFilter): Model

    /**
     * Count the number of [Model] that match the given [ModelFilter]
     * The [Project] filter is applied automatically.
     */
    fun count(filter: ModelFilter): Long
}

@Repository
class ModelJdbcDaoImpl : AbstractDao(), ModelJdbcDao {

    override fun count(filter: ModelFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: ModelFilter): Model {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Model not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun find(filter: ModelFilter): KPagedList<Model> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private val MAPPER = RowMapper { rs, _ ->
        Model(
            rs.getObject("pk_model") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getObject("pk_data_set") as UUID,
            ModelType.values()[rs.getInt("int_type")],
            rs.getString("str_name"),
            rs.getString("str_file_id"),
            rs.getString("str_job_name"),
            rs.getBoolean("bool_trained"),
            rs.getLong("time_created"),
            rs.getLong("time_modified"),
            rs.getString("actor_created"),
            rs.getString("actor_modified")
        )
    }

    companion object {

        const val GET = "SELECT * FROM model"
        const val COUNT = "SELECT COUNT(1) FROM model"
    }
}
