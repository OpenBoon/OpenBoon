package com.zorroa.archivist.repository

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.UserBase
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

interface UserDaoCache {
    fun getUser(id: Int): UserBase
    fun getUser(username: String): UserBase

}

@Repository
class UserDaoCacheImpl : AbstractDao(), UserDaoCache {

    private val cache = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .initialCapacity(32)
            .maximumSize(1024)
            .build(object : CacheLoader<Int, UserBase>() {
                @Throws(Exception::class)
                override fun load(key: Int): UserBase {
                    return jdbc.queryForObject(GET_BY_ID, MAPPER, key)
                }
            })

    override fun getUser(id: Int): UserBase {
        return cache.get(id)
    }

    override fun getUser(username: String): UserBase {
        return jdbc.queryForObject(GET_BY_NAME, MAPPER, username)
    }

    companion object {

        private val GET_BY_ID = "SELECT pk_user,str_username,str_email,pk_permission,pk_folder " +
                "FROM users WHERE pk_user=?"

        private val GET_BY_NAME = "SELECT pk_user,str_username,str_email,pk_permission,pk_folder " +
                "FROM users WHERE str_username=?"

        private val MAPPER = RowMapper<UserBase> { rs, _ ->
            UserBase(rs.getInt("pk_user"),
                    rs.getString("str_username"),
                    rs.getString("str_email"),
                    rs.getInt("pk_permission"),
                    rs.getInt("pk_folder"))
        }

    }
}
