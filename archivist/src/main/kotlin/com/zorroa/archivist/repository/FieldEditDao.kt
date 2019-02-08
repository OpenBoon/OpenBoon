package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.event
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface FieldEditDao {
    fun create(spec: FieldEditSpecInternal): FieldEdit
    fun get(id: UUID): FieldEdit
    fun get(assetId: UUID, fieldId: UUID) : FieldEdit
    fun delete(id: UUID) : Boolean
    fun getAll(assetId: UUID) : List<FieldEdit>
    fun getAll(filter: FieldEditFilter) : KPagedList<FieldEdit>
    fun count(filter: FieldEditFilter): Long

    /**
     * Return a map of manually edits can be applied to the asset.
     *
     * @param assetId The ID of the asset.
     * @return a map of manual edits with the attr as the key and value of the edit as the value.
     */
    fun getAssetUpdateMap(assetId: UUID) : Map<String, Any?>
}

@Repository
class FieldEditDaoImpl : AbstractDao(), FieldEditDao {

    @Autowired
    lateinit var userDaoCache: UserDaoCache

    override fun create(spec: FieldEditSpecInternal): FieldEdit {

        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, user.organizationId)
            ps.setObject(3, spec.fieldId)
            ps.setObject(4, spec.assetId)
            ps.setObject(5, user.id)
            ps.setObject(6, user.id)
            ps.setLong(7, time)
            ps.setLong(8, time)
            ps.setString(9, Json.serializeToString(
                    mapOf("value" to spec.oldValue), onNull = "{\"value\": null}"
            ))
            ps.setString(10, Json.serializeToString(
                    mapOf("value" to spec.newValue), onNull = "{\"value\": null}"
            ))
            ps
        }

        logger.event(LogObject.FIELD_EDIT, LogAction.CREATE,
                mapOf("fieldEditId" to id, "assetId" to spec.assetId, "fieldId" to spec.fieldId))

        return get(spec.assetId, spec.fieldId)
    }

    override fun get(id: UUID) : FieldEdit {
        return jdbc.queryForObject("$GET WHERE pk_organization=? AND pk_field_edit=?",
                MAPPER, getOrgId(), id)
    }

    override fun get(assetId: UUID, fieldId: UUID) : FieldEdit {
        return jdbc.queryForObject("$GET WHERE pk_field=? AND pk_asset=? AND pk_organization=?",
                MAPPER, fieldId, assetId, getOrgId())
    }

    override fun getAll(assetId: UUID) : List<FieldEdit> {
        return jdbc.query("$GET WHERE pk_asset=? AND pk_organization=?",
                MAPPER, assetId, getOrgId())
    }

    override fun count(filter: FieldEditFilter): Long {
        val query = filter.getQuery(COUNT, true)
        val values = filter.getValues(true)
        return jdbc.queryForObject(query, Long::class.java, *values)
    }

    override fun getAll(filter: FieldEditFilter) : KPagedList<FieldEdit> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun delete(id: UUID) : Boolean {
        val result =  jdbc.update(
                "DELETE FROM field_edit WHERE pk_organization=? AND pk_field_edit=?", getOrgId(), id) == 1
        logger.event(LogObject.FIELD_EDIT, LogAction.DELETE, mapOf("fieldEditId" to id, "boolStatus" to result))
        return result
    }

    override fun getAssetUpdateMap(assetId: UUID) : Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        jdbc.query(GET_UPDATE_MAP, RowCallbackHandler { rs->
            map[rs.getString(1)] =
                    Json.deserialize(rs.getString("json_new_value"), Json.GENERIC_MAP)["value"]
        }, assetId, getOrgId())
        return map
    }

    private val MAPPER = RowMapper { rs, _ ->
        FieldEdit(rs.getObject("pk_field_edit") as UUID,
                rs.getObject("pk_field") as UUID,
                rs.getObject("pk_asset") as UUID,
                Json.deserialize(rs.getString("json_old_value"), Json.GENERIC_MAP)["value"],
                Json.deserialize(rs.getString("json_new_value"), Json.GENERIC_MAP)["value"],
                rs.getLong("time_created"),
                userDaoCache.getUser(rs.getObject("pk_user_created") as UUID))
    }

    companion object {


        private const val GET = "SELECT * FROM field_edit"
        private const val COUNT = "SELECT COUNT(1) FROM field_edit"

        private val INSERT = JdbcUtils.insert("field_edit",
                "pk_field_edit",
                "pk_organization",
                "pk_field",
                "pk_asset",
                "pk_user_created",
                "pk_user_modified",
                "time_created",
                "time_modified",
                "json_old_value::jsonb",
                "json_new_value::jsonb")
                .plus(" ON CONFLICT (pk_field, pk_asset, pk_organization) DO UPDATE SET json_new_value=EXCLUDED.json_new_value,")
                .plus("pk_user_modified=EXCLUDED.pk_user_modified,")
                .plus("time_modified=EXCLUDED.time_modified")

        private const val GET_UPDATE_MAP =
                "SELECT " +
                    "field.str_attr_name,"+
                    "field_edit.json_new_value " +
                "FROM " +
                    "field_edit INNER JOIN field ON (field_edit.pk_field = field.pk_field) " +
                "WHERE " +
                    "field_edit.pk_asset=? " +
                "AND " +
                    "field_edit.pk_organization=?"
    }
}