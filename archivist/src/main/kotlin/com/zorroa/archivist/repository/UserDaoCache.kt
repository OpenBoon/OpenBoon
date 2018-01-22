package com.zorroa.archivist.repository

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class UserDaoCache {

    @Autowired
    internal var userDao: UserDao? = null

    private val cachedUserName = CacheBuilder.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .initialCapacity(32)
            .maximumSize(1024)
            .build(object : CacheLoader<Int, User>() {
                @Throws(Exception::class)
                override fun load(key: Int): User {
                    return userDao!!.get(key)
                }
            })

    fun getUser(id: Int): UserBase {
        return try {
            cachedUserName.get(id).setSettings(null)
        } catch (e: Exception) {
            UserBase().setUsername("unknown").setId(0)
        }
    }
}
