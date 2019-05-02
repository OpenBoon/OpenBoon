package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.*

interface SharedLinkDao {

    /**
     * Create a [SharedLink] with the given [SharedLinkSpec]
     *
     * @param spec A [SharedLinkSpec]
     */
    fun create(spec: SharedLinkSpec): SharedLink

    /**
     * Return a [SharedLink] by Id.
     *
     * @param id The unique ID of the [SharedLink]
     */
    fun get(id: UUID): SharedLink

    /**
     * Delete the [SharedLink]s older than the given duration. Return
     * the number of links deleted.
     *
     * @param duration The delete cutoff time.
     */
    fun deleteExpired(duration: Duration): Int
}

@Repository
class SharedLinkDaoImpl : AbstractDao(), SharedLinkDao {

    override fun create(spec: SharedLinkSpec): SharedLink {

        val key = GeneratedKeyHolder()
        val id = uuid1.generate()
        val user = getUser()

        jdbc.update({ connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, user.id)
            ps.setObject(3, user.organizationId)
            ps.setLong(4, System.currentTimeMillis())
            ps.setString(5, Json.serializeToString(spec.state, "{}"))
            ps.setString(6, Json.serializeToString(spec.userIds, "[]"))
            ps
        }, key)

        return get(id)
    }

    override fun get(id: UUID): SharedLink {
        return jdbc.queryForObject(
                "SELECT * FROM shared_link WHERE pk_organization=? AND pk_shared_link=?",
                MAPPER, getOrgId(), id)
    }

    override fun deleteExpired(duration: Duration): Int {
        logger.info("dur: {}", duration)
        val time = System.currentTimeMillis() - duration.toMillis()
        return jdbc.update("DELETE FROM shared_link WHERE time_created < ?", time)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            SharedLink(rs.getObject("pk_shared_link") as UUID,
                    Json.deserialize(rs.getString("json_state"), Json.GENERIC_MAP))
        }

        private val INSERT = JdbcUtils.insert("shared_link",
                "pk_shared_link",
                "pk_user_created",
                "pk_organization",
                "time_created",
                "json_state",
                "json_users")
    }
}
