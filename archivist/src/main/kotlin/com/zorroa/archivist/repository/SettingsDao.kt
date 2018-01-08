package com.zorroa.archivist.repository

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

interface SettingsDao {

    fun getAll(): Map<String, String>

    fun set(key: String, value: Any): Boolean

    fun unset(key: String): Boolean

    fun get(name: String): String
}

@Repository
open class SettingsDaoImpl : AbstractDao(), SettingsDao {

    override fun set(key: String, value: Any): Boolean {
        return if (isDbVendor("postgresql")) {
            jdbc.update(
                    "INSERT INTO settings (str_name, str_value) VALUES (?,?) ON CONFLICT(str_name) DO UPDATE SET str_value = ?",
                    key, value.toString(), value.toString()) == 1
        } else {
            jdbc.update("MERGE INTO settings (str_name, str_value) KEY(str_name) VALUES (?,?)",
                    key, value.toString()) == 1
        }
    }

    override fun unset(key: String): Boolean {
        return jdbc.update("DELETE FROM settings WHERE str_name=?", key) == 1
    }

    override fun get(name: String): String {
        return jdbc.queryForObject("SELECT str_value FROM settings WHERE str_name=?", String::class.java, name)
    }

    override fun getAll(): Map<String, String> {
        val result = Maps.newHashMap<String, String>()
        jdbc.query(GET_ALL) { rs ->
            result.put(rs.getString("str_name"),
                    rs.getString("str_value"))
        }
        return result
    }

    companion object {

        private val MAPPER = RowMapper<Map<String,String>> { rs, _ ->
            ImmutableMap.of<String, String>("key", rs.getString("str_name"),
                    "value", rs.getString("str_value"))
        }

        private val GET_ALL = "SELECT " +
                "* " +
                "FROM " +
                "settings "
    }
}
