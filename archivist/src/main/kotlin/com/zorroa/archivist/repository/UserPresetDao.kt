package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.UserPreset
import com.zorroa.archivist.domain.UserPresetSpec
import com.zorroa.archivist.domain.UserSettings
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository


/**
 * Created by chambers on 10/17/16.
 */
interface UserPresetDao : GenericNamedDao<UserPreset, UserPresetSpec>

/**
 * Created by chambers on 10/17/16.
 */
@Repository
open class UserPresetDaoImpl : AbstractDao(), UserPresetDao {

    private val MAPPER = RowMapper<UserPreset> { rs, _ ->
        val p = UserPreset()
        p.presetId = rs.getInt("pk_preset")
        p.name = rs.getString("str_name")
        p.permissionIds = getPermissons(rs.getInt("pk_preset"))
        p.settings = Json.deserialize<UserSettings>(rs.getString("json_settings"), UserSettings::class.java)
        p
    }

    private fun getPermissons(id: Int): List<Int> {
        return jdbc.queryForList("SELECT pk_permission FROM preset_permission  WHERE pk_preset=?",
                Int::class.java, id)
    }

    private fun setPermissions(presetId: Int, permissionIds: List<Int>?) {
        jdbc.update("DELETE FROM preset_permission WHERE pk_preset=?", presetId)
        if (permissionIds == null) {
            return
        }

        for (permId in permissionIds) {
            jdbc.update("INSERT INTO preset_permission (pk_preset, pk_permission) VALUES (?,?)",
                    presetId, permId)
        }
    }

    /**
     *
     * @param name
     * @return
     */
    override fun get(name: String): UserPreset {
        return jdbc.queryForObject<UserPreset>(GET + "WHERE str_name=?", MAPPER, name)
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject("SELECT COUNT(*) FROM preset WHERE str_name=?", Int::class.java, name) > 0
    }

    override fun create(spec: UserPresetSpec): UserPreset {
        val keyHolder = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_preset"))
            ps.setString(1, spec.name)
            ps.setString(2, Json.serializeToString(spec.settings, "{}"))
            ps
        }, keyHolder)
        val id = keyHolder.key.toInt()
        setPermissions(id, spec.permissionIds)

        return get(id)
    }

    override fun get(id: Int): UserPreset {
        return jdbc.queryForObject<UserPreset>(GET + "WHERE pk_preset=?", MAPPER, id)
    }

    override fun refresh(obj: UserPreset): UserPreset {
        return get(obj.presetId)
    }

    override fun getAll(): List<UserPreset> {
        return jdbc.query("SELECT * FROM preset", MAPPER)
    }

    override fun getAll(paging: Pager): PagedList<UserPreset> {
        return PagedList(paging.setTotalCount(count()),
                jdbc.query(GET + "ORDER BY preset.str_name LIMIT ? OFFSET ?", MAPPER,
                        paging.size, paging.from))
    }

    override fun update(id: Int, spec: UserPreset): Boolean {
        if (jdbc.update(UPDATE, spec.name, Json.serializeToString(spec.settings, "{}"), id) > 0) {
            setPermissions(id, spec.permissionIds)
            return true
        }
        return false
    }

    override fun delete(id: Int): Boolean {
        return jdbc.update("DELETE FROM preset WHERE pk_preset=?", id) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM preset", Long::class.java)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("preset",
                "str_name",
                "json_settings")

        private val GET = "SELECT " +
                "pk_preset," +
                "str_name," +
                "json_settings " +
                "FROM " +
                "preset "

        private val UPDATE = "UPDATE " +
                "preset " +
                "SET " +
                "str_name=?," +
                "json_settings=? " +
                "WHERE " +
                "pk_preset=?"
    }
}
