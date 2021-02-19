package boonai.archivist.repository

import com.fasterxml.jackson.module.kotlin.readValue
import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineModFilter
import boonai.archivist.domain.PipelineModUpdate
import boonai.archivist.security.getProjectId
import boonai.archivist.security.getZmlpActor
import boonai.archivist.util.JdbcUtils
import boonai.common.service.jpa.StringListConverter
import boonai.common.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface PipelineModDao {
    fun create(mod: PipelineMod)
    fun update(id: UUID, mod: PipelineModUpdate): Boolean
    fun delete(mod: PipelineMod): Boolean
    fun findOne(filter: PipelineModFilter): PipelineMod
    fun getAll(filter: PipelineModFilter): KPagedList<PipelineMod>
    fun count(filter: PipelineModFilter): Long
    fun get(id: UUID): PipelineMod
    fun getByName(name: String): PipelineMod
    fun findByName(name: String, standard: Boolean): PipelineMod?
    fun findByIdIn(ids: Collection<UUID>): List<PipelineMod>
    fun findByNameIn(names: Collection<String>): List<PipelineMod>
    fun removeStandardByIdNotIn(ids: Collection<UUID>): Int
}

@Repository
class PipelineModDaoImpl : PipelineModDao, AbstractDao() {

    override fun create(mod: PipelineMod) {
        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, mod.id)
            ps.setObject(2, mod.projectId)
            ps.setString(3, mod.name)
            ps.setString(4, mod.description)
            ps.setString(5, mod.provider)
            ps.setString(6, mod.category)
            ps.setString(7, mod.type)
            ps.setString(8, converter.convertToDatabaseColumn(mod.supportedMedia))
            ps.setObject(9, Json.serializeToString(mod.ops))
            ps.setLong(10, mod.timeCreated)
            ps.setLong(11, mod.timeModified)
            ps.setString(12, mod.actorCreated)
            ps.setString(13, mod.actorModified)
            ps
        }
    }

    override fun update(id: UUID, mod: PipelineModUpdate): Boolean {
        return jdbc.update(
            UPDATE,
            mod.name,
            mod.description,
            mod.provider,
            mod.category,
            mod.type,
            converter.convertToDatabaseColumn(mod.supportedMedia.map { it.name }),
            Json.serializeToString(mod.ops),
            System.currentTimeMillis(),
            getZmlpActor().toString(),
            id
        ) == 1
    }

    override fun delete(mod: PipelineMod): Boolean {
        return jdbc.update("DELETE FROM module WHERE pk_module=?", mod.id) == 1
    }

    override fun count(filter: PipelineModFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: PipelineModFilter): PipelineMod {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("PipelineMod not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getAll(filter: PipelineModFilter): KPagedList<PipelineMod> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun findByName(name: String, standard: Boolean): PipelineMod? {
        return try {
            if (standard) {
                jdbc.queryForObject(
                    "$GET WHERE str_name=? AND pk_project IS NULL LIMIT 1",
                    MAPPER, name
                )
            } else {
                jdbc.queryForObject(
                    "$GET WHERE str_name=? AND pk_project=? LIMIT 1",
                    MAPPER, name, getProjectId()
                )
            }
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun getByName(name: String): PipelineMod {
        return jdbc.queryForObject(
            "$GET WHERE str_name=? AND $PROJ_FILTER LIMIT 1",
            MAPPER, name, getProjectId()
        )
    }

    override fun get(id: UUID): PipelineMod {
        return jdbc.queryForObject(
            "$GET WHERE pk_module=? AND $PROJ_FILTER", MAPPER,
            id, getProjectId()
        )
    }

    override fun findByIdIn(ids: Collection<UUID>): List<PipelineMod> {
        if (ids.isEmpty()) {
            return listOf()
        }
        val inClause = JdbcUtils.inClause("pk_module", ids.size, "uuid")
        val args = mutableListOf<Any>()
        args.addAll(ids)
        args.add(getProjectId())

        return jdbc.query("$GET WHERE $inClause AND $PROJ_FILTER", MAPPER, *args.toTypedArray())
    }

    override fun findByNameIn(names: Collection<String>): List<PipelineMod> {
        if (names.isEmpty()) {
            return listOf()
        }
        val inClause = JdbcUtils.inClause("str_name", names.size)
        val args = mutableListOf<Any>()
        args.addAll(names)
        args.add(getProjectId())

        return jdbc.query("$GET WHERE $inClause AND $PROJ_FILTER", MAPPER, *args.toTypedArray())
    }

    override fun removeStandardByIdNotIn(ids: Collection<UUID>): Int {
        if (ids.isEmpty()) {
            return 0
        }
        val notInClause = JdbcUtils.inClause("pk_module", ids.size, comp = "NOT IN")
        val args = mutableListOf<Any>()
        args.addAll(ids)

        val res = jdbc.update(
            "DELETE FROM module WHERE pk_project IS NULL AND $notInClause",
            *args.toTypedArray()
        )
        logger.info("Removing $res standard modules, leftover= ${ids.size}")
        return res
    }

    companion object {
        const val GET = "SELECT * FROM module"
        const val COUNT = "SELECT COUNT(1) FROM module"
        const val PROJ_FILTER = "(module.pk_project=? OR pk_project IS NULL)"

        val INSERT = JdbcUtils.insert(
            "module",
            "pk_module",
            "pk_project",
            "str_name",
            "str_description",
            "str_provider",
            "str_category",
            "str_type",
            "str_supported_media",
            "json_ops::jsonb",
            "time_created",
            "time_modified",
            "actor_created",
            "actor_modified"
        )

        val UPDATE = JdbcUtils.update(
            "module",
            "pk_module",
            "str_name",
            "str_description",
            "str_provider",
            "str_category",
            "str_type",
            "str_supported_media",
            "json_ops::jsonb",
            "time_modified",
            "actor_modified"
        )

        private val converter = StringListConverter()

        private val MAPPER = RowMapper { rs, _ ->

            PipelineMod(
                rs.getObject("pk_module") as UUID,
                rs.getObject("pk_project") as UUID?,
                rs.getString("str_name"),
                rs.getString("str_description"),
                rs.getString("str_provider"),
                rs.getString("str_category"),
                rs.getString("str_type"),
                converter.convertToEntityAttribute(rs.getString("str_supported_media")) ?: listOf(),
                Json.Mapper.readValue(rs.getString("json_ops")),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified")
            )
        }
    }
}
