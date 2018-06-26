package com.zorroa.irm.studio.repository

import com.google.common.base.Preconditions
import com.zorroa.common.domain.Pipeline
import com.zorroa.common.domain.PipelineSpec
import com.zorroa.common.util.Json
import com.zorroa.common.util.Json.LIST_OF_PREFS
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
    fun count(): Long
    fun delete(id: UUID): Boolean
}

@Repository
class PipelineDaoImpl : AbstractJdbcDao(), PipelineDao {

    override fun create(spec: PipelineSpec): Pipeline {
        Preconditions.checkNotNull(spec.name)
        Preconditions.checkNotNull(spec.processors)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.name)
            ps.setString(3, Json.serializeToString(spec.processors, "[]"))
            ps.setLong(4, time)
            ps.setLong(5, time)
            ps
        })
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

    override fun update(pipeline: Pipeline): Boolean {
        return jdbc.update(UPDATE, pipeline.name,
                Json.serializeToString(pipeline.processors),
                pipeline.id) == 1
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM pipeline WHERE pk_pipeline=?", id, false) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM pipeline", Long::class.java)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            print(rs.getString("json_processors"))
            Pipeline(rs.getObject("pk_pipeline") as UUID,
                    rs.getString("str_name"),
                    rs.getLong("int_version"),
                    Json.Mapper.readValue(rs.getString("json_processors"), LIST_OF_PREFS))
        }

        private val INSERT = sqlInsert("pipeline",
                "pk_pipeline",
                "str_name",
                "json_processors",
                "time_created",
                "time_modified")

        private val UPDATE = sqlUpdate("pipeline", "pk_pipeline",
                "str_name",
                "json_processors")
    }
}



