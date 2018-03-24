package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.archivist.security.getUsername
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.processor.PipelineType
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.util.*


interface PipelineDao : GenericNamedDao<Pipeline, PipelineSpecV> {

    fun getStandard(type: PipelineType): Pipeline

    fun getAll(type: PipelineType): List<Pipeline>
}

@Repository
class PipelineDaoImpl : AbstractDao(), PipelineDao {

    override fun create(spec: PipelineSpecV): Pipeline {
        Preconditions.checkNotNull(spec.name)
        Preconditions.checkNotNull(spec.type)
        Preconditions.checkNotNull(spec.processors)

        if (spec.description == null) {
            spec.description = spec.name + " " +
                    spec.type + " pipeline created by " + getUsername()
        }


        if (spec.isStandard) {
            jdbc.update("UPDATE pipeline SET bool_standard=? WHERE bool_standard=? AND int_type=?",
                    false, true, spec.type.ordinal)
        }

        /*
         * If there are no pipelines, then this one is the standard.
         */
        if (count() == 0L) {
            spec.isStandard = true
        }

        val id = uuid1.generate()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.name)
            ps.setInt(3, spec.type.ordinal)
            ps.setString(4, Json.serializeToString(spec.processors, "[]"))
            ps.setString(5, spec.description)
            ps.setBoolean(6, spec.isStandard)
            ps
        })
        return get(id)
    }

    override fun getStandard(type: PipelineType): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>("SELECT * FROM pipeline WHERE bool_standard=? AND int_type=? LIMIT 1",
                    MAPPER, true, type.ordinal)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to find a standard pipeline", 1)
        }

    }

    override fun get(id: UUID): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>("SELECT * FROM pipeline WHERE pk_pipeline=?", MAPPER, id)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$id", 1)
        }

    }

    override fun get(name: String): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>("SELECT * FROM pipeline WHERE str_name=?", MAPPER, name)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$name", 1)
        }

    }

    override fun refresh(obj: Pipeline): Pipeline {
        return get(obj.id!!)
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline WHERE str_name=?", Int::class.java, name) == 1
    }

    override fun getAll(): List<Pipeline> {
        return jdbc.query("SELECT * FROM pipeline", MAPPER)
    }

    override fun getAll(type: PipelineType): List<Pipeline> {
        return jdbc.query("SELECT * FROM pipeline WHERE int_type=?", MAPPER, type.ordinal)
    }

    override fun getAll(paging: Pager): PagedList<Pipeline> {
        return PagedList(
                paging.setTotalCount(count()),
                jdbc.query<Pipeline>("SELECT * FROM pipeline ORDER BY pk_pipeline LIMIT ? OFFSET ?", MAPPER,
                        paging.size, paging.from))
    }

    override fun update(id: UUID, spec: Pipeline): Boolean {
        // Unset standard if its set.
        if (spec.isStandard) {
            jdbc.update("UPDATE pipeline SET bool_standard=? WHERE bool_standard=?",
                    false, true)
        }
        var incrementVersion = 0
        if (spec.versionUp != null && spec.versionUp!!) {
            incrementVersion = 1
        }

        return jdbc.update(UPDATE, spec.name,
                Json.serializeToString(spec.processors), spec.description,
                spec.isStandard, incrementVersion, id) == 1
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=? AND bool_standard=?", id, false) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline", Long::class.java)
    }

    companion object {

        private val MAPPER = RowMapper<Pipeline> { rs, _ ->
            val result = Pipeline()
            result.id = rs.getObject("pk_pipeline") as UUID
            result.name = rs.getString("str_name")
            result.processors = Json.deserialize<List<ProcessorRef>>(rs.getString("json_processors"), ProcessorRef.LIST_OF)
            result.type = PipelineType.fromObject(rs.getInt("int_type"))
            result.description = rs.getString("str_description")
            result.version = rs.getInt("int_version")
            result
        }

        private val INSERT = JdbcUtils.insert("pipeline",
                "pk_pipeline",
                "str_name",
                "int_type",
                "json_processors",
                "str_description",
                "bool_standard")

        private val UPDATE = JdbcUtils.update("pipeline", "pk_pipeline",
                "str_name",
                "json_processors",
                "str_description",
                "bool_standard",
                "int_version=int_version+?")
    }
}
