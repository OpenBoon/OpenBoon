package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Plugin
import com.zorroa.archivist.domain.Processor
import com.zorroa.archivist.domain.ProcessorFilter
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.plugins.ProcessorSpec
import com.zorroa.sdk.processor.ProcessorRef
import com.zorroa.sdk.processor.ProcessorType
import com.zorroa.sdk.util.Json
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface ProcessorDao {

    fun getAll(): List<Processor>
    fun exists(name: String): Boolean

    fun create(plugin: Plugin, spec: ProcessorSpec): Processor

    fun get(name: String): Processor

    fun get(id: Int): Processor

    fun refresh(`object`: Processor): Processor

    fun getAll(filter: ProcessorFilter): List<Processor>

    fun getAll(plugin: Plugin): List<Processor>

    fun getAll(page: Pager): PagedList<Processor>

    fun update(id: Int, spec: Processor): Boolean

    fun delete(id: Int): Boolean

    fun deleteAll(plugin: Plugin): Boolean

    fun count(): Long

    fun getRef(name: String): ProcessorRef
}

@Repository
class ProcessorDaoImpl : AbstractDao(), ProcessorDao {

    private val REF_MAPPER = RowMapper<ProcessorRef> { rs, _ ->
        val ref = ProcessorRef()
        ref.type = ProcessorType.values()[rs.getInt("int_type")]
        ref.className = rs.getString("str_name")
        ref.language = rs.getString("plugin_lang")
        ref.fileTypes = Json.deserialize<Set<String>>(rs.getString("json_file_types"), Json.SET_OF_STRINGS)
        ref.filters = mutableListOf()
        val filters = Json.deserialize<List<String>>(rs.getString("json_filters"), Json.LIST_OF_STRINGS)
        for (filt in filters) {
            ref.filters.add(com.zorroa.sdk.processor.ProcessorFilter(filt))
        }
        ref
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM processor WHERE str_name=?", Int::class.java, name) == 1
    }

    override fun create(plugin: Plugin, spec: ProcessorSpec): Processor {
        Preconditions.checkNotNull(spec.type, "The processor spec type cannot be null")
        Preconditions.checkNotNull(spec.className, "The processor class name cannot be null")
        Preconditions.checkNotNull(spec.display, "The processor cannot have a null display property")

        if (!spec.className.contains(".")) {
            throw IllegalArgumentException("Processor class name has no module, must be named 'something.Name'")
        }

        val shortName = spec.className.substring(spec.className.lastIndexOf('.') + 1)
        val module = spec.className.substring(0, spec.className.lastIndexOf('.'))

        val time = System.currentTimeMillis()
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_processor"))
            ps.setInt(1, plugin.id)
            ps.setString(2, spec.className)
            ps.setString(3, shortName)
            ps.setString(4, module)
            ps.setInt(5, spec.type.ordinal)
            ps.setString(6, if (spec.description == null) shortName else spec.description)
            ps.setString(7, Json.serializeToString(spec.display, "[]"))
            ps.setString(8, Json.serializeToString(spec.filters, "[]"))
            ps.setString(9, Json.serializeToString(spec.fileTypes, "[]"))
            ps.setLong(10, time)
            ps.setLong(11, time)
            ps
        }, keyHolder)
        val id = keyHolder.key.toInt()
        return get(id)
    }

    override operator fun get(name: String): Processor {
        return jdbc.queryForObject<Processor>(GET + " WHERE processor.str_name=?", MAPPER, name)
    }

    override operator fun get(id: Int): Processor {
        return jdbc.queryForObject<Processor>(GET + " WHERE processor.pk_processor=?", MAPPER, id)
    }

    override fun refresh(`object`: Processor): Processor {
        return get(`object`.id)
    }

    override fun getAll(): List<Processor> {
        return jdbc.query(GET + " ORDER BY processor.str_short_name", MAPPER)
    }

    override fun getAll(filter: ProcessorFilter): List<Processor> {

        if (!JdbcUtils.isValid(filter.sort)) {
            filter.sort = (ImmutableMap.of("shortName", "asc"))
        }
        val q = filter.getQuery(GET, null)
        return jdbc.query<Processor>(q, MAPPER, *filter.getValues())
    }

    override fun getAll(plugin: Plugin): List<Processor> {
        return jdbc.query<Processor>(GET + " WHERE plugin.pk_plugin=?", MAPPER, plugin.id)
    }

    override fun getAll(page: Pager): PagedList<Processor> {
        return PagedList(
                page.setTotalCount(count()),
                jdbc.query<Processor>(GET + "ORDER BY processor.str_short_name LIMIT ? OFFSET ?", MAPPER,
                        page.size, page.from))
    }

    override fun update(id: Int, spec: Processor): Boolean {
        return false
    }

    override fun delete(id: Int): Boolean {
        return jdbc.update("DELETE FROM processor WHERE processor.pk_processor=?", id) > 0
    }


    override fun deleteAll(plugin: Plugin): Boolean {
        return jdbc.update("DELETE FROM processor WHERE processor.pk_plugin=?", plugin.id) > 0
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM processor", Long::class.java)
    }

    override fun getRef(name: String): ProcessorRef {
        try {
            return jdbc.queryForObject<ProcessorRef>(GET_REF, REF_MAPPER, name)
        } catch (e: EmptyResultDataAccessException) {
            throw EmptyResultDataAccessException("Failed to find Processor '$name'", 1, e)
        }

    }

    companion object {

        private val INSERT = JdbcUtils.insert("processor",
                "pk_plugin",
                "str_name",
                "str_short_name",
                "str_module",
                "int_type",
                "str_description",
                "json_display",
                "json_filters",
                "json_file_types",
                "time_created",
                "time_modified")

        private val MAPPER = RowMapper<Processor> { rs, _ ->
            val p = Processor()
            p.id = rs.getInt("pk_processor")
            p.name = rs.getString("str_name")
            p.shortName = rs.getString("str_short_name")
            p.module = rs.getString("str_module")
            p.type = ProcessorType.values()[rs.getInt("int_type")]
            p.description = rs.getString("str_description")
            p.display = Json.deserialize<List<Map<String, Any>>>(rs.getString("json_display"), object : TypeReference<List<Map<String, Any>>>() {

            })
            p.filters = Json.deserialize<List<String>>(rs.getString("json_filters"), Json.LIST_OF_STRINGS)
            p.fileTypes = Json.deserialize<Set<String>>(rs.getString("json_file_types"), Json.SET_OF_STRINGS)
            p.pluginId = rs.getInt("pk_plugin")
            p.pluginLanguage = rs.getString("plugin_lang")
            p.pluginVersion = rs.getString("plugin_ver")
            p.pluginName = rs.getString("plugin_name")
            p
        }

        private val GET = "SELECT " +
                "processor.pk_processor," +
                "processor.str_name," +
                "processor.str_short_name," +
                "processor.str_module," +
                "processor.int_type," +
                "processor.str_description," +
                "processor.json_display," +
                "processor.json_filters, " +
                "processor.json_file_types," +
                "plugin.pk_plugin, " +
                "plugin.str_name AS plugin_name, " +
                "plugin.str_lang AS plugin_lang, " +
                "plugin.str_version AS plugin_ver " +
                "FROM " +
                "processor JOIN plugin ON ( processor.pk_plugin = plugin.pk_plugin ) "

        private val GET_REF = "SELECT " +
                "processor.str_name," +
                "processor.int_type," +
                "processor.json_filters, " +
                "processor.json_file_types," +
                "plugin.str_lang AS plugin_lang " +
                "FROM " +
                "processor INNER JOIN plugin ON ( processor.pk_plugin = plugin.pk_plugin ) " +
                "WHERE " +
                "processor.str_name=?"
    }
}
