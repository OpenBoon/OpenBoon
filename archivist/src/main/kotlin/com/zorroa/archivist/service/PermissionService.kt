package com.zorroa.archivist.service

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.common.domain.PagedList
import com.zorroa.common.domain.Pager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

interface PermissionService {

     fun getPermissions(): List<Permission>

     fun getPermissions(page: Pager, filter: PermissionFilter): PagedList<Permission>

     fun getPermissions(page: Pager): PagedList<Permission>

     fun getUserAssignablePermissions(page: Pager): PagedList<Permission>

     fun getObjAssignablePermissions(page: Pager): PagedList<Permission>

     fun getPermission(id: UUID): Permission

     fun createPermission(builder: PermissionSpec): Permission

     fun getPermissionNames(): List<String>

     fun permissionExists(authority: String): Boolean

     fun getPermission(name: String): Permission

     fun deletePermission(permission: Permission): Boolean
}

@Service
@Transactional
class PermissionServiceImpl @Autowired constructor(
        private val permissionDao : PermissionDao,
        private val txem: TransactionEventManager
) : PermissionService {

    @Autowired
    private lateinit var logService : EventLogService

    private val permissionCache = CacheBuilder.newBuilder()
            .maximumSize(200)
            .initialCapacity(100)
            .concurrencyLevel(4)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(object : CacheLoader<String, Permission>() {
                @Throws(Exception::class)
                override fun load(key: String): Permission {
                    return if (key.contains(Permission.JOIN)) {
                        permissionDao.get(key)
                    } else {
                        permissionDao.get(UUID.fromString(key))
                    }
                }
            })


    override fun getPermissions(): List<Permission> {
        return permissionDao.getAll()
    }

    override fun getPermissions(page: Pager): PagedList<Permission> {
        return permissionDao.getPaged(page)
    }

    override fun getPermissions(page: Pager, filter: PermissionFilter): PagedList<Permission> {
        return permissionDao.getPaged(page, filter)
    }

    override fun getUserAssignablePermissions(page: Pager): PagedList<Permission> {
        return permissionDao.getPaged(page,
                PermissionFilter()
                        .setAssignableToUser(true)
                        .forceSort(ImmutableMap.of("str_group", "asc", "str_type", "asc")))
    }

    override fun getObjAssignablePermissions(page: Pager): PagedList<Permission> {
        return permissionDao.getPaged(page,
                PermissionFilter()
                        .setAssignableToObj(true)
                        .forceSort(ImmutableMap.of("str_group", "asc", "str_type", "asc")))
    }

    override fun getPermissionNames(): List<String> {
        return permissionDao.getAll().stream().map({ p -> p.fullName }).collect(Collectors.toList<String>())
    }

    override fun permissionExists(authority: String): Boolean {
        return permissionDao.exists(authority)
    }

    override fun getPermission(name: String): Permission {
        try {
            return permissionCache.get(name)
        } catch (e: Exception) {
            throw EmptyResultDataAccessException("The permission $name does not exist", 1)
        }

    }

    // CACHE
    override fun getPermission(id: UUID): Permission {
        return permissionDao.get(id)
    }

    override fun createPermission(builder: PermissionSpec): Permission {
        val perm = permissionDao.create(builder, false)
        txem.afterCommit(true, { logService.logAsync(UserLogSpec.build(LogAction.Create, perm)) })
        return perm
    }

    override fun deletePermission(permission: Permission): Boolean {
        val result = permissionDao.delete(permission)
        if (result) {
            txem.afterCommit(true, { logService.logAsync(UserLogSpec.build(LogAction.Delete, permission)) })
        }
        return result
    }
}
