package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.sdk.services.AssetId
import com.zorroa.archivist.sdk.services.AssetSpec
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.security.getUserId
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface AssetDao {
    fun create(spec: AssetSpec) : AssetId
    fun getId(id: UUID) : AssetId
}

@Repository
class AssetDaoPostgresImpl : AbstractDao(), AssetDao {

    override fun getId(id: UUID) : AssetId {
        return jdbc.queryForObject("$GET_ID WHERE pk_organization=? AND pk_asset=?",
                MAPPER_ASSET_ID, getUser().organizationId, id)
    }

    override fun create(spec: AssetSpec): AssetId {
        val time = System.currentTimeMillis()

        val id = uuid1.generate()
        val userid = getUserId()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, getUser().organizationId)
            ps.setString(3, spec.filename)
            ps.setLong(4, time)
            ps.setLong(5, time)
            ps.setObject(6, userid)
            ps.setObject(7, userid)
            ps
        })

        return AssetId(id, getUser().organizationId, spec.filename)
    }

    companion object {

        private val MAPPER_ASSET_ID= RowMapper<AssetId> { rs, _ ->
            AssetId(rs.getObject("pk_asset") as UUID,
                    rs.getObject("pk_organization") as UUID,
                    rs.getString("str_filename"))
        }

        private const val GET_ID = "SELECT pk_asset, pk_organization,str_filename FROM asset"

        private val INSERT = JdbcUtils.insert("asset",
                "pk_asset",
                "pk_organization",
                "str_filename",
                "time_created",
                "time_modified",
                "pk_user_created",
                "pk_user_modified")
    }
}


