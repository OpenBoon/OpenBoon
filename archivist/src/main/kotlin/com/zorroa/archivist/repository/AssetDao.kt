package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.AssetId
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface AssetDao {
    fun create(spec: AssetSpec) : AssetId
    fun exists(location: String?) : Boolean
    fun getId(location: String) : AssetId
    fun getId(id: UUID) : AssetId
}

@Repository
class AssetDaoPostgresImpl : AbstractDao(), AssetDao {

    override fun getId(location: String) : AssetId {
        return jdbc.queryForObject("SELECT pk_asset, pk_organization, int_state FROM asset WHERE pk_organization=? AND str_location=?",
                MAPPER_ASSET_ID, getUser().organizationId, location)
    }

    override fun getId(id: UUID) : AssetId {
        return jdbc.queryForObject("SELECT pk_asset, pk_organization, int_state FROM asset WHERE pk_organization=? AND pk_asset=?",
                MAPPER_ASSET_ID, getUser().organizationId, id)
    }

    override fun exists(location: String?) : Boolean {
        return if (location == null) {
            false
        }
        else {
            jdbc.queryForObject("SELECT COUNT(1) FROM asset WHERE pk_organization=? AND str_location=?",
                    Int::class.java, getUser().organizationId, location) == 1
        }
    }

    override fun create(spec: AssetSpec): AssetId {
        val time = System.currentTimeMillis()

        val id = if (spec.location != null) {
            uuid3.generate(spec.location)
        }
        else {
            uuid1.generate()
        }

        val state = if (spec.directAccess) {
            AssetState.PENDING
        }
        else {
            AssetState.PENDING_FILE
        }

        val userid = getUserId()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, getUser().organizationId)
            ps.setString(3, spec.filename)
            ps.setString(4, spec.location)
            ps.setInt(5, state.ordinal)
            ps.setBoolean(6, spec.directAccess)
            ps.setLong(7, time)
            ps.setLong(8, time)
            ps.setObject(9, userid)
            ps.setObject(10, userid)
            ps
        })

        return AssetId(id, getUser().organizationId, state)
    }


    companion object {

        private val MAPPER_ASSET_ID= RowMapper<AssetId> { rs, _ ->
            AssetId(rs.getObject("pk_asset") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    AssetState.values()[rs.getInt("int_state")])
        }

        private val INSERT = JdbcUtils.insert("asset",
                "pk_asset",
                "pk_organization",
                "str_filename",
                "str_location",
                "int_state",
                "bool_direct",
                "time_created",
                "time_modified",
                "pk_user_created",
                "pk_user_modified")

    }

}


