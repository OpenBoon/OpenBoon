package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.google.common.collect.Maps
import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.Plugin
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.plugins.PluginSpec
import org.apache.commons.lang3.NotImplementedException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

interface PluginDao : GenericNamedDao<Plugin, PluginSpec> {

    /**
     * Get a map of plugin name/md5 sum for quickly checking if a plugin
     * version is installed or not.
     *
     * @return
     */
    fun getInstalledVersions(): Map<String, String>

    fun update(id: Int, spec: PluginSpec): Boolean
}

@Repository
open class PluginDaoImpl : AbstractDao(), PluginDao {

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(1) FROM plugin WHERE str_name=?", Int::class.java, name) > 0
    }

    override fun create(spec: PluginSpec): Plugin {
        Preconditions.checkNotNull(spec.name, "Plugin name is null" + spec.name)
        Preconditions.checkNotNull(spec.description, "Plugin description is null" + spec.name)
        Preconditions.checkNotNull(spec.version, "Plugin version is null" + spec.name)
        Preconditions.checkNotNull(spec.publisher, "Plugin publisher is null: " + spec.name)
        Preconditions.checkNotNull(spec.md5, "Plugin md5 is null: " + spec.name)

        val time = System.currentTimeMillis()
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_plugin"))
            ps.setString(1, spec.name)
            ps.setString(2, spec.version)
            ps.setString(3, spec.language)
            ps.setString(4, spec.description)
            ps.setString(5, spec.publisher)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setString(8, spec.md5)
            ps
        }, keyHolder)
        val id = keyHolder.key.toInt()
        return get(id)
    }

    override fun get(id: Int): Plugin {
        return jdbc.queryForObject<Plugin>(GET + " WHERE pk_plugin=?", MAPPER, id)
    }

    override fun get(name: String): Plugin {
        return jdbc.queryForObject<Plugin>(GET + " WHERE str_name=?", MAPPER, name)
    }

    override fun refresh(obj: Plugin): Plugin {
        return get(obj.id)
    }

    override fun getAll(): List<Plugin> {
        return jdbc.query(GET, MAPPER)
    }

    override fun getInstalledVersions(): Map<String, String> {
        val result = Maps.newHashMap<String, String>()
        jdbc.query("SELECT str_name, str_md5 FROM plugin") { rs ->
            result.put(rs.getString("str_name"),
                    rs.getString("str_md5"))
        }
        return result
    }

    override fun getAll(paging: Pager): PagedList<Plugin> {
        return PagedList(
                paging.setTotalCount(count()),
                jdbc.query<Plugin>(GET + "ORDER BY str_name LIMIT ? OFFSET ?", MAPPER,
                        paging.size, paging.from))
    }

    override fun update(id: Int, spec: Plugin): Boolean {
        throw NotImplementedException("Not implemented, try 'boolean update(int id, PluginSpec spec)'")
    }

    override fun update(id: Int, spec: PluginSpec): Boolean {
        return jdbc.update(UPDATE, spec.version, System.currentTimeMillis(), spec.md5, id) == 1
    }

    override fun delete(id: Int): Boolean {
        return jdbc.update("DELETE FROM plugin WHERE pk_plugin=?", id) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM plugin", Long::class.java)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("plugin",
                "str_name",
                "str_version",
                "str_lang",
                "str_description",
                "str_publisher",
                "time_created",
                "time_modified",
                "str_md5")

        private val MAPPER = RowMapper<Plugin> { rs, _ ->
            val result = Plugin()
            result.id = rs.getInt("pk_plugin")
            result.name = rs.getString("str_name")
            result.description = rs.getString("str_description")
            result.language = rs.getString("str_lang")
            result.version = rs.getString("str_version")
            result.publisher = rs.getString("str_publisher")
            result.md5 = rs.getString("str_md5")
            result
        }

        private val GET = "SELECT " +
                "pk_plugin," +
                "str_name," +
                "str_version," +
                "str_description," +
                "str_publisher," +
                "str_lang, " +
                "str_md5 " +
                "FROM " +
                "plugin "

        private val UPDATE = JdbcUtils.update("plugin", "pk_plugin", "str_version", "time_modified", "str_md5")
    }
}
