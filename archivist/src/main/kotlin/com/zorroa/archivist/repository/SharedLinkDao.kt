package com.zorroa.archivist.repository

import com.zorroa.archivist.JdbcUtils
import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.security.getUserId
import com.zorroa.sdk.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

interface SharedLinkDao {

    fun create(spec: SharedLinkSpec): SharedLink

    fun get(id: Int): SharedLink

    fun deleteExpired(olderThan: Long): Int
}

@Repository
class SharedLinkDaoImpl : AbstractDao(), SharedLinkDao {

    private val MAPPER = RowMapper<SharedLink> { rs, _ ->
        val link = SharedLink()
        link.id = rs.getInt("pk_shared_link")
        link.state = Json.deserialize<Map<String, Any>>(rs.getString("json_state"),
                Json.GENERIC_MAP)
        link.expireTime = rs.getLong("time_expired")
        link
    }

    override fun create(spec: SharedLinkSpec): SharedLink {

        val defaultExpireTimeMs = TimeUnit.HOURS.toMillis(
                properties.getInt("archivist.maintenance.sharedLinks.expireDays").toLong())

        val expireTime: Long = if (spec.expireTimeMs != null) {
            System.currentTimeMillis() + spec.expireTimeMs!!
        } else {
            System.currentTimeMillis() + defaultExpireTimeMs
        }

        val key = GeneratedKeyHolder()
        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT, arrayOf("pk_shared_link"))
            ps.setInt(1, getUserId())
            ps.setLong(2, System.currentTimeMillis())
            ps.setLong(3, expireTime)
            ps.setString(4, Json.serializeToString(spec.state, "{}"))
            ps.setString(5, Json.serializeToString(spec.userIds, "[]"))
            ps
        }, key)

        val id = key.key.toInt()
        return get(id)
    }

    override operator fun get(id: Int): SharedLink {
        return jdbc.queryForObject("SELECT * FROM shared_link WHERE pk_shared_link=?",
                MAPPER, id)
    }

    override fun deleteExpired(olderThan: Long): Int {
        return jdbc.update("DELETE FROM shared_link WHERE time_expired < ?", olderThan)
    }

    companion object {

        private val INSERT = JdbcUtils.insert("shared_link",
                "pk_user",
                "time_created",
                "time_expired",
                "json_state",
                "json_users")
    }
}
