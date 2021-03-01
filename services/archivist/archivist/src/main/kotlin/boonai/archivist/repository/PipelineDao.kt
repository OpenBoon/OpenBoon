package boonai.archivist.repository

import boonai.archivist.domain.Pipeline
import boonai.archivist.domain.PipelineFilter
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineMode
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.PipelineUpdate
import boonai.archivist.domain.ProcessorRef
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.util.JdbcUtils.insert
import boonai.archivist.util.isUUID
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface PipelineDao {
    fun create(spec: PipelineSpec): Pipeline
    fun get(name: String): Pipeline
    fun getDefault(): Pipeline
    fun get(id: UUID): Pipeline
    fun update(id: UUID, update: PipelineUpdate): Boolean
    fun refresh(obj: Pipeline): Pipeline
    fun delete(id: UUID): Boolean
    fun getAll(filter: PipelineFilter): KPagedList<Pipeline>
    fun findOne(filter: PipelineFilter): Pipeline
    fun count(filter: PipelineFilter): Long
    fun setPipelineMods(id: UUID, mods: List<PipelineMod>)
}

@Repository
class PipelineDaoImpl : AbstractDao(), PipelineDao {

    override fun create(spec: PipelineSpec): Pipeline {

        val id = uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()
        val projectId = spec.projectId ?: getProjectId()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, projectId)
            ps.setString(3, spec.name)
            ps.setInt(4, spec.mode.ordinal)
            ps.setString(5, Json.serializeToString(spec.processors, "[]"))
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setString(8, actor)
            ps.setString(9, actor)
            ps
        }

        logger.event(
            LogObject.PIPELINE, LogAction.CREATE,
            mapOf("pipelineId" to id, "pipelineName" to spec.name)
        )
        return get(id)
    }

    override fun setPipelineMods(id: UUID, mods: List<PipelineMod>) {
        jdbc.update("DELETE FROM x_module_pipeline WHERE pk_pipeline=?", id)
        mods?.forEach {
            jdbc.update(
                "INSERT INTO x_module_pipeline (pk_module, pk_pipeline) VALUES (?, ?)",
                it.id, id
            )
        }
    }

    override fun getDefault(): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>(
                GET_DEF,
                MAPPER, getProjectId()
            )
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get default pipeline", 1)
        }
    }

    override fun get(id: UUID): Pipeline {
        try {
            return jdbc.queryForObject<Pipeline>(
                "$GET WHERE pk_pipeline=? AND pk_project=?",
                MAPPER, id, getProjectId()
            )
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$id", 1)
        }
    }

    override fun get(name: String): Pipeline {
        if (name.isUUID()) {
            return get(UUID.fromString(name))
        }
        try {
            return jdbc.queryForObject<Pipeline>(
                "$GET WHERE pk_project=? AND str_name=? ",
                MAPPER, getProjectId(), name
            )
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to get pipeline: id=$name", 1)
        }
    }

    override fun refresh(obj: Pipeline): Pipeline {
        return get(obj.id)
    }

    override fun update(id: UUID, update: PipelineUpdate): Boolean {
        var updates = jdbc.update(
            UPDATE, update.name, Json.serializeToString(update.processors), id, getProjectId()
        )

        update.modules?.let {
            jdbc.update("DELETE FROM x_module_pipeline WHERE pk_pipeline=?", id)
            it.forEach { mod ->
                updates += jdbc.update(
                    "INSERT INTO x_module_pipeline (pk_module, pk_pipeline) VALUES (?,?)",
                    mod, id
                )
            }
        }

        return updates > 0
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update(
            "DELETE FROM pipeline WHERE pk_pipeline=? AND pk_project=?", id, getProjectId()
        ) == 1
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

    private val MAPPER = RowMapper { rs, _ ->
        Pipeline(
            rs.getObject("pk_pipeline") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getString("str_name"),
            PipelineMode.values()[rs.getInt("int_mode")],
            Json.Mapper.readValue(rs.getString("json_processors"), ProcessorRef.LIST_OF),
            jdbc.queryForList(
                "SELECT x.pk_module FROM x_module_pipeline x WHERE x.pk_pipeline=?", UUID::class.java,
                rs.getObject("pk_pipeline")
            ),
            rs.getLong("time_created"),
            rs.getLong("time_modified"),
            rs.getString("actor_created"),
            rs.getString("actor_modified")
        )
    }

    companion object {

        private const val GET = "SELECT * FROM pipeline"
        private const val GET_DEF = "SELECT pipeline.* FROM pipeline " +
            "INNER JOIN project ON project.pk_pipeline_default = pipeline.pk_pipeline " +
            "WHERE project.pk_project=?"
        private const val COUNT = "SELECT COUNT(1) FROM pipeline"
        private const val UPDATE = "UPDATE " +
            "pipeline " +
            "SET " +
            "str_name=?, " +
            "json_processors=?::jsonb " +
            "WHERE " +
            "pk_pipeline=? AND pk_project=?"

        private val INSERT = insert(
            "pipeline",
            "pk_pipeline",
            "pk_project",
            "str_name",
            "int_mode",
            "json_processors::jsonb",
            "time_created",
            "time_modified",
            "actor_created",
            "actor_modified"
        )
    }
}
