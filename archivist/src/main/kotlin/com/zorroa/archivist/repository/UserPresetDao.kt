package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.UserPreset
import com.zorroa.archivist.domain.UserPresetSpec
import com.zorroa.archivist.domain.UserSettings
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*


/**
 * Created by chambers on 10/17/16.
 */
interface UserPresetDao : GenericNamedDao<UserPreset, UserPresetSpec>

/**
 * Created by chambers on 10/17/16.
 */
@Repository
class UserPresetDaoImpl : AbstractDao(), UserPresetDao {

    private val MAPPER = RowMapper<UserPreset> { rs, _ ->
        val p = UserPreset()
        p.presetId = rs.getObject("pk_preset") as UUID
        p.name = rs.getString("str_name")
        p.permissionIds = getPermissons(rs.getObject("pk_preset") as UUID)
        p.settings = Json.deserialize<UserSettings>(rs.getString("json_settings"), UserSettings::class.java)
        p
    }

    private fun getPermissons(id: UUID): List<UUID> {
        return jdbc.queryForList("SELECT pk_permission FROM preset_permission  WHERE pk_preset=?",
                UUID::class.java, id)
    }

    private fun setPermissions(presetId: UUID, permissionIds: List<UUID>?) {
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
        val id = uuid1.generate()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setString(2, spec.name)
            ps.setString(3, Json.serializeToString(spec.settings, "{}"))
            ps
        })
        setPermissions(id, spec.permissionIds)
        return get(id)
    }

    override fun get(id: UUID): UserPreset {
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

    override fun update(id: UUID, spec: UserPreset): Boolean {
        if (jdbc.update(UPDATE, spec.name, Json.serializeToString(spec.settings, "{}"), id) > 0) {
            setPermissions(id, spec.permissionIds)
            return true
        }
        return false
    }

    override fun delete(id: UUID): Boolean {
        return jdbc.update("DELETE FROM preset WHERE pk_preset=?", id) == 1
    }

    override fun count(): Long {
        return jdbc.queryForObject("SELECT COUNT(1) FROM preset", Long::class.java)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("preset",
                "pk_preset",
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
