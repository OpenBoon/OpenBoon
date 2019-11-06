package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.LIST_OF_PREFS
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.ZpsSlot
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.JdbcUtils.insert
import com.zorroa.archivist.util.JdbcUtils.isUUID
import com.zorroa.archivist.util.JdbcUtils.update
import com.zorroa.archivist.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface PipelineDao {
    fun create(spec: PipelineSpec): Pipeline
    fun get(name: String): Pipeline
    fun get(id: UUID): Pipeline
    fun update(pipeline: Pipeline): Boolean
    fun refresh(obj: Pipeline): Pipeline
    fun delete(id: UUID): Boolean
    fun getAll(filter: PipelineFilter): KPagedList<Pipeline>
    fun findOne(filter: PipelineFilter): Pipeline
    fun count(filter: PipelineFilter): Long
}

@Repository
class PipelineDaoImpl : AbstractDao(), PipelineDao {

    override fun create(spec: PipelineSpec): Pipeline {
        Preconditions.checkNotNull(spec.name)
        Preconditions.checkNotNull(spec.processors)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, getProjectId())
            ps.setString(3, spec.name)
            ps.setInt(4, spec.slot.ordinal)
            ps.setString(5, Json.serializeToString(spec.processors, "[]"))
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps
        }
        logger.event(
            LogObject.PIPELINE, LogAction.CREATE,
            mapOf("pipelineId" to id, "pipelineName" to spec.name)
        )
        return get(id)
    }

    override fun get(id: UUID): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>(
                "$GET WHERE pk_pipeline=? AND project_id=?",
                MAPPER, id, getProjectId()
            )
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$id", 1)
        }
    }

    override fun get(name: String): Pipeline {
        if (isUUID(name)) {
            return get(UUID.fromString(name))
        }
        try {
            return jdbc.queryForObject<Pipeline>(
                "$GET WHERE project_id=? AND str_name=? ",
                MAPPER, getProjectId(), name
            )
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$name", 1)
        }
    }

    override fun refresh(obj: Pipeline): Pipeline {
        return get(obj.id)
    }

    override fun update(pipeline: Pipeline): Boolean {
        return jdbc.update(
            UPDATE, pipeline.name,
            Json.serializeToString(pipeline.processors),
            pipeline.id
        ) == 1
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update(
            "DELETE FROM pipeline WHERE pk_pipeline=? AND project_id=?", id, getProjectId()) == 1
    }

    override fun getAll(filter: PipelineFilter): KPagedList<Pipeline> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(filter: PipelineFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    override fun findOne(filter: PipelineFilter): Pipeline {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Pipeline not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Pipeline(
                rs.getObject("pk_pipeline") as UUID,
                rs.getString("str_name"),
                ZpsSlot.values()[rs.getInt("int_slot")],
                Json.Mapper.readValue(rs.getString("json_processors"), LIST_OF_PREFS)
            )
        }

        private val GET = "SELECT * FROM pipeline"
        private val COUNT = "SELECT COUNT(1) FROM pipeline"

        private val INSERT = insert(
            "pipeline",
            "pk_pipeline",
            "project_id",
            "str_name",
            "int_slot",
            "json_processors",
            "time_created",
            "time_modified"
        )

        private val UPDATE = update(
            "pipeline", "pk_pipeline",
            "str_name",
            "json_processors"
        )
    }
}
