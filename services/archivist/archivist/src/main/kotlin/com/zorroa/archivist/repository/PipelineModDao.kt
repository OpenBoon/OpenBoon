package com.zorroa.archivist.repository

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModFilter
import com.zorroa.archivist.domain.SupportedMedia
import com.zorroa.zmlp.service.jpa.StringListConverter
import com.zorroa.zmlp.util.Json
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PipelineModDao : JpaRepository<PipelineMod, UUID> {

    /**
     * Get a [PipelineMod] by name or return null.
     */
    fun getByName(name: String): PipelineMod?

    /**
     * Get the list of mods by Id.  It may be better to use the [PipelineModService.getByIds]
     * method which throws if all Ids are not found.
     */
    fun findByIdIn(ids: Collection<UUID>): List<PipelineMod>

    /**
     * Get the list of mods by name.  It may be better to use the [PipelineModService.getByNames]
     * method which throws if all Ids are not found.
     */
    fun findByNameIn(names: Collection<String>): List<PipelineMod>
}

interface PipelineModCustomDao {
    fun findOne(filter: PipelineModFilter): PipelineMod
    fun getAll(filter: PipelineModFilter): KPagedList<PipelineMod>
    fun count(filter: PipelineModFilter): Long
}

@Repository
class PipelineModCustomDaoImpl : PipelineModCustomDao, AbstractDao() {

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

    companion object {
        const val GET = "SELECT * FROM module"
        const val COUNT = "SELECT COUNT(1) FROM module"
        private val converter = StringListConverter()

        private val MAPPER = RowMapper { rs, _ ->

            PipelineMod(
                rs.getObject("pk_module") as UUID,
                rs.getString("str_name"),
                rs.getString("str_description"),
                rs.getString("str_provider"),
                rs.getString("str_category"),
                converter.convertToEntityAttribute(rs.getString("str_supported_media")) ?: listOf(),
                rs.getBoolean("bool_restricted"),
                Json.Mapper.readValue(rs.getString("json_ops")),
                rs.getLong("time_created"),
                rs.getLong("time_modified"),
                rs.getString("actor_created"),
                rs.getString("actor_modified")
            )
        }
    }
}
