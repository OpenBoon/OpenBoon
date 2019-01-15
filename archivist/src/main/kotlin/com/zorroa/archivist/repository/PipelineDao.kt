package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.service.event
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils.insert
import com.zorroa.common.util.JdbcUtils.isUUID
import com.zorroa.common.util.JdbcUtils.update
import com.zorroa.common.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface PipelineDao {
    fun create(spec: PipelineSpec): Pipeline
    fun get(name: String) : Pipeline
    fun get(id: UUID) : Pipeline
    fun update(pipeline: Pipeline) : Boolean
    fun exists(name: String) : Boolean
    fun refresh(obj: Pipeline): Pipeline
    fun getAll(): List<Pipeline>
    fun getAll(type:PipelineType): List<Pipeline>
    fun getAll(paging: Pager): PagedList<Pipeline>
    fun count(): Long
    fun delete(id: UUID): Boolean
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
            ps.setString(2, spec.name)
            ps.setInt(3, spec.type.ordinal)
            ps.setString(4, Json.serializeToString(spec.processors, "[]"))
            ps.setString(5, spec.description)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps
        }
        logger.event(LogObject.PIPELINE, LogAction.CREATE,
                mapOf("pipelineId" to id, "pipelineName" to spec.name))
        return get(id)
    }

    override fun get(id: UUID): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>("SELECT * FROM pipeline WHERE pk_pipeline=?", MAPPER, id)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$id", 1)
        }
    }

    override fun get(name: String): Pipeline {
        if (isUUID(name)) {
            return get(UUID.fromString(name))
        }
        try {
            return jdbc.queryForObject<Pipeline>("SELECT * FROM pipeline WHERE str_name=?", MAPPER, name)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$name", 1)
        }
    }

    override fun refresh(obj: Pipeline): Pipeline {
        return get(obj.id)
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline WHERE str_name=?", Int::class.java, name) == 1
    }

    override fun getAll(): List<Pipeline> {
        return jdbc.query("SELECT * FROM pipeline", MAPPER)
    }

    override fun getAll(type:PipelineType): List<Pipeline> {
        return jdbc.query("SELECT * FROM pipeline WHERE int_type=?", MAPPER, type.ordinal)
    }

    override fun getAll(paging: Pager): PagedList<Pipeline> {
        return PagedList(
                paging.setTotalCount(count()),
                jdbc.query<Pipeline>("SELECT * FROM pipeline ORDER BY pk_pipeline LIMIT ? OFFSET ?", MAPPER,
                        paging.size, paging.from))
    }

    override fun update(pipeline: Pipeline): Boolean {
        return jdbc.update(UPDATE, pipeline.name,
                Json.serializeToString(pipeline.processors),
                pipeline.id) == 1
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=?", id) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline", Long::class.java)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Pipeline(rs.getObject("pk_pipeline") as UUID,
                    rs.getString("str_name"),
                    PipelineType.values()[rs.getInt("int_type")],
                    Json.Mapper.readValue(rs.getString("json_processors"), LIST_OF_PREFS))
        }

        private val INSERT = insert("pipeline",
                "pk_pipeline",
                "str_name",
                "int_type",
                "json_processors",
                "str_description",
                "time_created",
                "time_modified")

        private val UPDATE = update("pipeline", "pk_pipeline",
                "str_name",
                "json_processors")
    }
}



