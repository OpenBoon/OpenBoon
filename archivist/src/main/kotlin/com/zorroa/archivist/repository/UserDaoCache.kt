package com.zorroa.archivist.repository

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.UserBase
import com.zorroa.archivist.security.SuperAdmin
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.TimeUnit

interface UserDaoCache {
    fun getUser(id: UUID): UserBase
    fun getUser(username: String): UserBase
    fun invalidate(id: UUID)

}

@Repository
class UserDaoCacheImpl : AbstractDao(), UserDaoCache {

    private val cache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .initialCapacity(32)
            .maximumSize(1024)
            .build(object : CacheLoader<UUID, UserBase>() {
                @Throws(Exception::class)
                override fun load(key: UUID): UserBase {
                    // Super admin isn't in the DB so we'll return a static record
                    return if (key == SuperAdmin.id) {
                        SuperAdmin.base
                    }
                    else {
                        jdbc.queryForObject(GET_BY_ID, MAPPER, key)
                    }
                }
            })

    override fun invalidate(id: UUID) {
        return cache.invalidate(id)
    }

    override fun getUser(id: UUID): UserBase {
        return cache.get(id)
    }

    override fun getUser(username: String): UserBase {
        return jdbc.queryForObject(GET_BY_NAME, MAPPER, username, username)
    }

    companion object {

        private const val GET = "SELECT pk_user,str_username,str_email,pk_permission,pk_folder,pk_organization " +
                "FROM users"
        private const val GET_BY_ID = "$GET WHERE pk_user=?"

        private const val GET_BY_NAME = "$GET WHERE (str_username=? OR str_email=?)"

        private val MAPPER = RowMapper { rs, _ ->
            UserBase(rs.getObject("pk_user") as UUID,
                    rs.getString("str_username"),
                    rs.getString("str_email"),
                    rs.getObject("pk_permission") as UUID?,
                    rs.getObject("pk_folder") as UUID?,
                    rs.getObject("pk_organization") as UUID)
        }

    }
}
